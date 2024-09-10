package com.DataService;

import com.cloudpss.eventchain.*;
import com.cloudpss.model.JobArguments;
import com.cloudpss.model.Model;
import com.cloudpss.runner.Runner;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.spy.memcached.MemcachedClient;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.Future;

public class CloudpssRTU {

    private Properties config;
    private Properties controlconfig;
    private HashMap<String,String> map;
    private HashMap<String,String> controlMap;
    private MemcachedClient client;
    private String IP="166.111.60.221";
    private String taskid=null;

    public CloudpssRTU()
    {
        InputStream fis=this.getClass().getClassLoader().getResourceAsStream("RTU.properties");
        config = new Properties();
        try {
            config.load(fis);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        map = new HashMap<String,String>();
        Iterator<String> it = config.stringPropertyNames().iterator();
        while(it.hasNext())
        {
            String key = it.next();
            //System.out.println("key="+key+" value = "+"/"+config.getProperty(key)+":0");
            map.put(key,"/"+config.getProperty(key)+":0");
        }

        fis=this.getClass().getClassLoader().getResourceAsStream("control.properties");
        controlconfig = new Properties();
        try {
            controlconfig.load(fis);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        controlMap = new HashMap<String,String>();
        Iterator<String> it2 = controlconfig.stringPropertyNames().iterator();
        while(it2.hasNext())
        {
            String key = it2.next();
            //System.out.println("key="+key+" value = "+controlconfig.getProperty(key));
            controlMap.put(key,controlconfig.getProperty(key));
        }


    }

    public HashMap<String,String> getMap()
    {
        return map;
    }

    public void setData(String key,String value)
    {
        if(taskid==null)
            return;

        String key1 = controlMap.get(key);
        if(key1==null)
            return;
        ControlParam ctrlCell = new ControlParam(key1,value,new EventMessage(key+" is set to "+value));
        SimulationControl control = new SimulationControl(taskid);
        control.control(ctrlCell);
    }

    public void getData() {
        try {
            client = new MemcachedClient(new InetSocketAddress("127.0.0.1", 11211));
            Future fo = client.set("TestMemcached", 300, 1);
            System.out.println("set status:" + fo.get());
            System.out.println("Connection to server successful.");


            // ????????????????token
            String token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6NjQ2LCJ1c2VybmFtZSI6IkFtZWVyIiwic2NvcGVzIjpbIm1vZGVsOjk4MzY3IiwiZnVuY3Rpb246OTgzNjciLCJhcHBsaWNhdGlvbjozMjgzMSJdLCJyb2xlcyI6WyJBbWVlciJdLCJ0eXBlIjoiYXBwbHkiLCJleHAiOjE3NTU4NDY2MjQsIm5vdGUiOiJzZGsiLCJpYXQiOjE3MjQ3NDI2MjR9.Cage0p6r7fD2a7AOHXEsTNNHPKFD9leOQ2rOCx_hjPD2iYiBAFu92PjS8UrfnKgSGEUIe2DvA4VMsnDQ2oy07g";
            System.setProperty("CLOUDPSS_API_URL", "http://166.111.60.221:60002/");

            System.setProperty("CLOUDPSS_TOKEN", token);
            String rid = "model/Ameer/IEEE39";
            // ??????????
            Model model = Model.fetch(rid);
            JobArguments job=model.getJobs().get(1);

            // ????????
            Runner runner = model.run(job);
            taskid = runner.getTaskId();
            //System.out.println("TaskId = "+ taskid );
            client.set("TASKID", 300, taskid);
            int n=0;
            // ????›¥??? result ??????????????????????result ??????????
            while (runner.status() == 0)
            {
                System.out.println("Simulation is running. Waiting ...... "+n);
                Thread.sleep(1000);
                n++;
                if (!runner.result.hasNext())
                    continue;

                JsonParser jParser = new JsonParser();
                int lastId = runner.result.getMessage().size() - 1;
                if (lastId < 10)
                    continue;
                // ?????i?????????
                String msg = runner.result.getMessage().get(lastId);
                //System.out.println(msg);

                JsonObject jts = (JsonObject) jParser.parse(msg);
                if (!jts.get("cmd").getAsString().equals("draw"))
                    continue;
                // ?????i??????????§Ö?data;
                JsonObject jt = (JsonObject) jParser.parse(jts.get("data").toString());
                if (jt == null)
                    continue;
                //System.out.println(jt);
                for (String key : map.keySet()) {
                    String datakey = map.get(key);
                    if (jt.get(datakey) == null)
                    {
                        System.out.println("datakey error: "+datakey);
                        continue;
                    }
                    JsonArray a = jt.get(datakey).getAsJsonArray();
                    int l = a.size();
                    JsonArray b = a.get(l - 1).getAsJsonArray();
                    if(key.equals("K2f"))
                        System.out.println("\tt = " + new DecimalFormat("0.000").format(b.get(0).getAsDouble())+" ");
                    //System.out.println("\tval= " + b.get(1).getAsDouble());
                    double v= b.get(1).getAsDouble();
                    if(key.contains("VT")) {
                        String val = new DecimalFormat("0.000").format(v);
                        client.set(key, 300, val);
                    }
                    if(key.contains("K"))
                    {
                        if(Math.abs(v-1.0)<1e-4)
                            client.set(key,300, "1");
                        else
                            client.set(key,300, "0");
                        //System.out.println("key = " +key+ " datakey= "+datakey+ " val= "+ v);
                    }
                    else
                    {
                        String val = new DecimalFormat("0.000").format(v);
                        //System.out.println("key = " +key+ " datakey= "+datakey+ " val= "+ val);
                        client.set(key, 300, val); // §Õ??
                    }


                }
                //System.out.println("write data to memcached");
				/*
				if(n==15)
					setData("K2f","0");
				else if(n==25)
					setData("K2f","1");
				*/
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally
        {
            client.shutdown();
        }

    }

    public static void main(String[] args) throws Exception {
        CloudpssRTU rtu = new CloudpssRTU();
        rtu.getData();
    }
}
