package com.demo.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.demo.entity.AbstractSnmp;
import com.demo.entity.HrSWRunPerfTable;
import com.demo.entity.HrStorageTable;
import com.demo.service.AbstractSnmpService;
import com.demo.service.HrSWRunPerfEntryService;
import com.demo.service.HrSWRunPerfTableService;
import com.demo.service.HrStorageTableService;
import com.demo.util.SnmpUtil;

@Controller
public class HostingController {
	
	public static long BtoGB = 1024*1024*1024;
	
	@RequestMapping(value = {"/", "/index"}, method = RequestMethod.GET)
    public String input(Model model) {
		return "index";
	}
	
	@RequestMapping(value = "/result", method = RequestMethod.POST)
    public String show(Model model, @ModelAttribute AbstractSnmp abs) {
		List<HrStorageTable> tables1 = new ArrayList<HrStorageTable>();
		List<HrSWRunPerfTable> tables2 = new ArrayList<HrSWRunPerfTable>();
	    boolean check = false;
		if (abs.getCommunity() != "" && abs.getHost() != "") {
			try {
				// Get hrStorageTable, OID = .1.3.6.1.2.1.25.2.3
				AbstractSnmpService abstractSnmpService1 = new HrStorageTableService(abs.getHost(), abs.getCommunity(), SnmpUtil.getSNMPVersionInteger(abs.getVersion()));
		        Object o1 = abstractSnmpService1.getResult().getObj();
		        if (o1 instanceof String[][]) {
		            String[][] str = (String[][]) o1;
		            for (int i = 0; i < str.length; i++) {
		    			HrStorageTable table = new HrStorageTable();
		    			table.setIndex(Integer.parseInt(str[i][0]));
		    			table.setDescr(str[i][2]);
		    			table.setSize(Math.round(((Double.parseDouble(str[i][4])*Long.parseLong(str[i][3]))/BtoGB)*100.0)/100.0);
		    			table.setUsed(Math.round(((Double.parseDouble(str[i][5])*Long.parseLong(str[i][3]))/BtoGB)*100.0)/100.0);
		    			tables1.add(table);
		    		}
		            check = true;
		            model.addAttribute("tables1", tables1);
		            System.out.println("GET STORAGE SUCCESS!");
		        }
		        // Get hrSWRunEntry, OID = .1.3.6.1.2.1.25.4.2.1
		        AbstractSnmpService abstractSnmpService2 = new HrSWRunPerfTableService(abs.getHost(), abs.getCommunity(), SnmpUtil.getSNMPVersionInteger(abs.getVersion()));
		        Object o2 = abstractSnmpService2.getResult().getObj();
		        // Get hrSWRunPerfTable, OID = .1.3.6.1.2.1.25.5.1.1
		        AbstractSnmpService abstractSnmpService3 = new HrSWRunPerfEntryService(abs.getHost(), abs.getCommunity(), SnmpUtil.getSNMPVersionInteger(abs.getVersion()));
		        Object o3 = abstractSnmpService3.getResult().getObj();
		        if (o2 instanceof String[][]) {
		            String[][] str = (String[][]) o2;
		            String[][] str1 = (String[][]) o3;
//		            double cpu = 0;
//		            double ram = 0;
		            for (int i = 0; i < str.length; i++) {
		    			HrSWRunPerfTable table = new HrSWRunPerfTable();
		    			table.setIndex(Integer.parseInt(str[i][0]));
		    			table.setName(str[i][1]);
		    			table.setId(str[i][2]);
		    			table.setPath(str[i][3]);
		    			table.setParam(str[i][4]);
		    			table.setType(str[i][5]);
		    			table.setStatus(str[i][6]);
		    			table.setPriority(str[i][7]);
//		    			table.setDescr(str[i][2]);
//		    			table.setSize(Math.round(((Double.parseDouble(str[i][4])*Long.parseLong(str[i][3]))/BtoGB)*100.0)/100.0);
//		    			table.setUsed(Math.round(((Double.parseDouble(str[i][5])*Long.parseLong(str[i][3]))/BtoGB)*100.0)/100.0);
		    			table.setPerfCpu(str1[i][0]);
		    			table.setPerfMem(Math.round((Double.parseDouble(str1[i][1])/1024)*100.0)/100.0);
		    			tables2.add(table);
//		    			cpu = cpu + Double.parseDouble(str1[i][0]);
//		    			ram = ram + Double.parseDouble(str1[i][1]);
		    		}
//		            Collections.sort(tables2);
		            check = true;
		            model.addAttribute("tables2", tables2);
		            System.out.println("GET USAGE SUCCESS!");
		        }
			} catch (Exception e) {
				// TODO: handle exception
				System.err.println("ERROR!");
			}
		}
		model.addAttribute("check", check);
        return "result";
    }
	
}
