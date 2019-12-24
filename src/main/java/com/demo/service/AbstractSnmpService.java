package com.demo.service;

import com.demo.entity.SnmpResult;
import com.demo.util.SnmpUtil;

public abstract class AbstractSnmpService {
	
	private String host;
    private String community;
    private Integer version;

    public AbstractSnmpService() {
    }

    public AbstractSnmpService(String host, String community, Integer version) {
        this.host = host;
        this.community = community;
        this.version = version;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getCommunity() {
        return community;
    }

    public void setCommunity(String community) {
        this.community = community;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public abstract String getOid();

    public abstract SnmpResult getResult();

    public String getShell() {
        return String.format("snmpwalk -v%s -c%s %s %s", SnmpUtil.getSNMPVersionString(getVersion()), getCommunity(), getHost(), getOid());
    }
}
