package eu.liveandgov.wp1.backend.SensorValueObjects;

import eu.liveandgov.wp1.backend.format.SampleType;

public class RawSensorValue extends SensorValue {
	public String value;
	
	public RawSensorValue(SampleType type, long timestamp, String id, String value) {
		this.type = type;
		this.timestamp = timestamp;
		this.id = id;
		this.value = value;
	}
	
	public String toString(){
		return String.format("RSV - type:%s ts:%d id:%s val:%s", type, timestamp, id, value);
	}
}