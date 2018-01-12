package com.iss.bigdata.health.elasticsearch.entity;

import java.util.Date;
import java.util.Map;

/**
 * Created by dujijun on 2018/1/3.
 */
public class Allergy {
    private long timestamp;
    private String user_id;
    private Date start;
    private String encounter;
    private Map<String, String> allergies;

    public Allergy() {
    }

    public Allergy(long timestamp, String user_id, Date start, String encounter, Map<String, String> allergies) {

        this.timestamp = timestamp;
        this.user_id = user_id;
        this.start = start;
        this.encounter = encounter;
        this.allergies = allergies;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public String getEncounter() {
        return encounter;
    }

    public void setEncounter(String encounter) {
        this.encounter = encounter;
    }

    public Map<String, String> getAllergies() {
        return allergies;
    }

    public void setAllergies(Map<String, String> allergies) {
        this.allergies = allergies;
    }

    @Override
    public String toString() {
        return "Allergy{" +
                "timestamp=" + timestamp +
                ", user_id='" + user_id + '\'' +
                ", start=" + start +
                ", encounter='" + encounter + '\'' +
                ", allergies=" + allergies +
                '}';
    }
}
