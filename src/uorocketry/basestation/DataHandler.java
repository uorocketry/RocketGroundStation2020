package uorocketry.basestation;

import java.util.LinkedList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.json.JSONException;
import org.json.JSONObject;

public class DataHandler {
	
	static final DataType TIMESTAMP = new DataType(0, 0);
	static final DataType ALTITUDE = new DataType(1, 0);
	static final DataType LATITUDE = new DataType(2, 0);
	static final DataType LONGITUDE = new DataType(3, 0);
	static final DataType PITCH = new DataType(4, 0);
	static final DataType ROLL = new DataType(5, 0);
	static final DataType YAW = new DataType(6, 0);
	static final DataType ACCELX = new DataType(7, 0);
	static final DataType ACCELY = new DataType(8, 0);
	static final DataType ACCELZ = new DataType(9, 0);
	static final DataType VELOCITY = new DataType(10, 0);
	static final DataType BRAKE_PERCENTAGE = new DataType(10, 0);
	static final DataType ACTUAL_BRAKE_VALUE = new DataType(12, 0);
	static final DataType GPS_FIX = new DataType(13, 0);
	static final DataType GPS_FIX_QUALITY = new DataType(14, 0);
	
	Data[] data;
	DataType[] types;
	
	/** Which of this data should be hidden for any reason */
	List<DataType> hiddenDataTypes = new LinkedList<DataType>();
	
	/**
	 * This chooses which table this data is displayed in
	 */
	int tableIndex = 0;
	
	public DataHandler(int tableIndex) {
		this.tableIndex = tableIndex;
		
		this.data = new Data[Main.dataLength.get(tableIndex)];
		
		types = new DataType[Main.dataLength.get(tableIndex)];
		for (int i = 0; i < types.length; i++) {
			types[i] = new DataType(i, tableIndex);
		}
	}
	
	public void updateTableUIWithData(JTable table, String[] labels) {
		TableModel tableModel = table.getModel();
		
		for (int i = 0; i < data.length; i++) {
			// Set label
			tableModel.setValueAt(labels[i], i, 0);
			
			String dataText = data[i].getFormattedString();
			if (hiddenDataTypes.contains(types[i])) dataText = "Hidden Data";
			
			// Hardcode for now TODO: Move this into config
			if (i == data.length - 1 && labels[i].toLowerCase().contains("state")) {
				switch (dataText) {
				case "0":
					dataText = "Init";
					break;
				case "1":
					dataText = "Wait For Init";
					break;
				case "2":
					dataText = "Wait For Launch";
					break;
				case "3":
					dataText = "Powered Flight";
					break;
				case "4":
					dataText = "Coast";
					break;
				case "5":
					dataText = "Descent Phase 1";
					break;
				case "6":
					dataText = "Descent Phase 2";
					break;
				case "7":
					dataText = "Ground";
					break;
				case "8":
					dataText = "Max States";
					break;
				}
			}
			
			// Set data
			tableModel.setValueAt(dataText, i, 1);
		}
	}
	
	public boolean set(int index, String currentData, JSONObject dataset) {
		// Check for special cases first
		boolean isFormattedCoordinate = false;
		boolean isTimestamp = false;
		try {
			isTimestamp = dataset.getInt("timestampIndex") == index;
			
			JSONObject coordinateIndexes = dataset.getJSONObject("coordinateIndexes");
			isFormattedCoordinate = coordinateIndexes.has("formattedCoordinates") 
					&& coordinateIndexes.getBoolean("formattedCoordinates") 
					&& (coordinateIndexes.getInt("latitude") == index || coordinateIndexes.getInt("longitude") == index);
		} catch (JSONException e) {}
		
		if (isFormattedCoordinate) {
			// These need to be converted to decimal coordinates to be used
			
			float degrees = 0;
			float minutes = 0;
			
			int minutesIndex = currentData.indexOf(".") - 2;
			//otherwise, it is badly formatted and probably still zero
			if (minutesIndex >= 0) {
				minutes = Float.parseFloat(currentData.substring(minutesIndex, currentData.length()));
				degrees = Float.parseFloat(currentData.substring(0, minutesIndex));
			}
			
			data[index] = new Data(degrees, minutes);
		} else if (isTimestamp) {
			// Long case
			long longData = -1;
			
			try {
				longData = Long.parseLong(currentData);
			} catch (NumberFormatException e) {
				if (currentData.equals("ovf")) {
					// ovf means overflow
					longData = Long.MAX_VALUE;
				} else {
					System.err.println("Number conversion failed for '" + currentData + "', -1 being used instead");
					
					return false;
				}
			}
			
			data[index] = new Data(longData);
		} else {
			// Normal case
			float floatData = -1;
			
			try {
				floatData = Float.parseFloat(currentData);
			} catch (NumberFormatException e) {
				if (currentData.equals("ovf")) {
					// ovf means overflow
					floatData = Float.MAX_VALUE;
				} else {
					System.err.println("Number conversion failed for '" + currentData + "', -1 being used instead");
					
					return false;
				}
			}
			
			data[index] = new Data(floatData);
		}
		
		return true;
	}
}
