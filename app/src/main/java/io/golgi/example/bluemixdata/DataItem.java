package io.golgi.example.bluemixdata;

        import com.ibm.mobile.services.data.IBMDataObject;
        import com.ibm.mobile.services.data.IBMDataObjectSpecialization;

/*
 * Created by ianh on 20/10/2014.
 *
 * Item for writing/reading to Bluemix Cloud data
 *
 */
@IBMDataObjectSpecialization("DataItem")
public class DataItem extends IBMDataObject {
    public static final String VALUE = "VALUE";

    public void setValue(String value){
        setObject(VALUE,value);
    }

    public String getValue(){
        return (String)getObject(VALUE);
    }
}
