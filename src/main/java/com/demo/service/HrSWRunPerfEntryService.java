package com.demo.service;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.snmp4j.smi.VariableBinding;

import com.demo.entity.SnmpResult;
import com.demo.util.SnmpUtil;

public class HrSWRunPerfEntryService extends AbstractSnmpService {
	
private static String oid = ".1.3.6.1.2.1.25.5.1.1";
	
	public HrSWRunPerfEntryService(String host, String community, Integer version) {
        super(host, community, version);
    }

    @Override
    public String getOid() {
        return oid;
    }

    @Override
    public SnmpResult getResult() {
        Vector<VariableBinding> variableBindings = SnmpUtil.snmpWalk(getHost(), getCommunity(), getOid(), getVersion());
        if (variableBindings == null || variableBindings.size() == 0) {
            return null;
        }

        List<Map<String, Object>> walkList = SnmpUtil.getWalkList(variableBindings, 1);
        String[] indexs = {"1", "2"};
        String[][] obj = new String[walkList.size()][indexs.length];

        for (int i = 0; i < walkList.size(); i++) {
            Map<String, Object> stringObjectMap = walkList.get(i);
            for (int i1 = 0; i1 < indexs.length; i1++) {
                obj[i][i1] = stringObjectMap.getOrDefault(indexs[i1], "").toString();
            }
        }

        return new SnmpResult(obj);
    }
}
