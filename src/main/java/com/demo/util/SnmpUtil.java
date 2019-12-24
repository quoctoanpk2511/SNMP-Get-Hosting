package com.demo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.*;

public class SnmpUtil {
  private static Logger log = LoggerFactory.getLogger(SnmpUtil.class);

  public static CommunityTarget createDefault(String ip, String community, Integer snmpVersion) {
      Address address = GenericAddress.parse("udp:" + ip + "/161");
      CommunityTarget target = new CommunityTarget();
      target.setCommunity(new OctetString(community));
      target.setAddress(address);
      target.setVersion(snmpVersion);
      target.setTimeout(3000);
      target.setRetries(2);
      return target;
  }

  /**
   * @param ip
   * @param community
   * @param targetOid
   * @param snmpVersion
   */
  public static Vector<VariableBinding> snmpWalk(String ip, String community, String targetOid, Integer snmpVersion) {
      Vector<VariableBinding> vector = new Vector<>();
      // Target
      CommunityTarget target = createDefault(ip, community, snmpVersion);

      // Transport protocol
      TransportMapping transport = null;
      // Create SNMP to send request PDU
      Snmp snmp = null;
      try {
          // Set the transmission protocol to UDP
          transport = new DefaultUdpTransportMapping();
          snmp = new Snmp(transport);
          // Start the listening process and receive messages
          transport.listen();

          // Create request pdu, get mib
          PDU pdu = new PDU();
          // Create OID instance
          OID targetOID = new OID(targetOid);
          // Bound to the OID to be queried
          pdu.add(new VariableBinding(targetOID));

          // Declared finished = false
          boolean finished = false;
          while (!finished) {
              VariableBinding vb = null;
              // 
              ResponseEvent respEvent = snmp.getNext(pdu, target);

              // Gets the response PDU
              PDU response = respEvent.getResponse();

              // Response is empty?
              if (null == response) {
                  finished = true;
                  break;
              } else {
                  // Gets the variable binding at the specified position
                  vb = response.get(0);
              }

              // check finish
              finished = checkWalkFinished(targetOID, pdu, vb);
              if (!finished) {
                  vector.add(vb);
                  // Set up the variable binding for the next entry.
                  pdu.setRequestID(new Integer32(0));
                  pdu.set(0, vb);
              } else {
                  snmp.close();
              }
          }
      } catch (Exception e) {
          e.printStackTrace();
      } finally {
          closeSnmp(snmp);
          closeTransportMapping(transport);
      }
      return vector;
  }

  /**
   * @param variableBindings walk back result set
   * @param lastIndex        以最后几位是索引值 默认是1位,s93是3位,需要从监控项的配置参数里获取
   * @return
   */
  public static List<Map<String, Object>> getWalkList(Vector<VariableBinding> variableBindings, int lastIndex) {
      lastIndex = lastIndex == 0 ? 1 : lastIndex;
      List<Map<String, Object>> list = new ArrayList<>();
      HashMap<String, Map<String, Object>> stringMapHashMap = new HashMap<>();

      for (VariableBinding variableBinding : variableBindings) {
          String oid = variableBinding.getOid().toString();
          String[] numbers = oid.split("\\.");
          StringBuilder stringBuilder = new StringBuilder();
          for (int index = lastIndex; index > 0; index--) {
              stringBuilder.append(numbers[numbers.length - index]);
          }
          String indexString = stringBuilder.toString();
          String type = numbers[numbers.length - lastIndex - 1];
          if (!stringMapHashMap.containsKey(indexString)) {
              Map<String, Object> map = new HashMap<>();
              map.put(type, SnmpUtil.getFormatStr(variableBinding.getVariable().toString()));
              stringMapHashMap.put(indexString, map);
              continue;
          }
          Map<String, Object> stringObjectMap = stringMapHashMap.get(indexString);
          if (!stringObjectMap.containsKey(type)) {
              stringObjectMap.put(type, SnmpUtil.getFormatStr(variableBinding.getVariable().toString()));
          }
      }
      stringMapHashMap.forEach((k, v) -> list.add(v));
      return list;
  }

  public static String getFormatStr(String octetString) {
      return getFormatStr(octetString, "iso-8859-1");
  }

  private static String getFormatStr(String octetString, String charset) {
      try {
          if (!octetString.contains(":")) {
              return octetString;
          }
          String[] temps = octetString.split(":");
          byte[] bs = new byte[temps.length];
          for (int i = 0; i < temps.length; i++) {
              bs[i] = (byte) Integer.parseInt(temps[i], 16);
          }
          return new String(bs, charset);
      } catch (Exception e) {
          return octetString;
      }
  }

  private static boolean checkWalkFinished(OID targetOID, PDU pdu, VariableBinding vb) {
      boolean finished = false;
      if (pdu.getErrorStatus() != 0) {
          log.debug("[true] responsePDU.getErrorStatus() != 0 ");
          log.debug(pdu.getErrorStatusText());
          finished = true;
      } else if (vb.getOid() == null) {
          log.debug("[true] vb.getOid() == null");
          finished = true;
      } else if (vb.getOid().size() < targetOID.size()) {
          log.debug("[true] vb.getOid().size() < targetOID.size()");
          finished = true;
      } else if (targetOID.leftMostCompare(targetOID.size(), vb.getOid()) != 0) {
          log.debug("[true] targetOID.leftMostCompare() != 0");
          finished = true;
      } else if (Null.isExceptionSyntax(vb.getVariable().getSyntax())) {
          log.debug("[true] Null.isExceptionSyntax(vb.getVariable().getSyntax())");
          finished = true;
      } else if (vb.getOid().compareTo(targetOID) <= 0) {
          log.debug("[true] Variable received is not lexicographic successor of requested " + "one:");
          log.debug(vb.toString() + " <= " + targetOID);
          finished = true;
      }
      return finished;
  }

  public static void closeSnmp(Snmp snmp) {
      if (snmp == null) {
          return;
      }
      try {
          snmp.close();
      } catch (IOException e) {
          e.printStackTrace();
      }
  }

  public static void closeTransportMapping(TransportMapping transportMapping) {
      if (transportMapping == null) {
          return;
      }
      try {
          transportMapping.close();
      } catch (IOException e) {
          e.printStackTrace();
      }
  }

  public static Integer getSNMPVersionInteger(String version) {
      switch (version) {
          case "1":
              return SnmpConstants.version1;
          case "2c":
              return SnmpConstants.version2c;
          case "3":
              return SnmpConstants.version3;
          default:
              throw new IllegalArgumentException("Unsupported version number");
      }
  }

  public static String getSNMPVersionString(Integer version) {
      switch (version) {
          case SnmpConstants.version1:
              return "1";
          case SnmpConstants.version2c:
              return "2c";
          case SnmpConstants.version3:
              return "3";
          default:
              throw new IllegalArgumentException("Unsupported version number");
      }
  }
}