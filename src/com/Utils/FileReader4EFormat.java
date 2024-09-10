package com.Utils;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;


public class FileReader4EFormat {

    final static String TAGSTARTER = "TagStarter";
    final static String TAGENDER = "TagEnder";
    final static String COMMENTSTARTER = "CommentStarter";
    final static String ATTRIBUTENAMESTARTER = "AttributeNameStarter";
    final static String DATALINESTARTER = "DataLineStarter";
    final static String ATTRIBUTEBREAKER = "AttributeBreaker";
    final static String DATABREAKER = "DataBreaker";

    private String configFile = null;
    private Properties configPro = new Properties();
    private String sourceFile = null;
    private BufferedReader br = null;
    private InputStreamReader inRead=null;
    private String currentTag = null;
    private String currentDataLine = null;
    private List<String> currentAttList = new ArrayList<String>();
    private boolean isRealEnd = false;
    private Map<String, String> qsProp = new HashMap<String, String>();

    private HashMap<String, HashMap<String, Object>> dataMap=null;
    private HashMap<String, HashMap<String, Object>> ieeeDataMap=null;

    public Properties getConfigPro() {
        return configPro;
    }

    public boolean setConfigFile(InputStream fis) {
        //this.configFile = fileName;
        if (fis == null)
            return false;
        try {
            InputStreamReader fr= new InputStreamReader(fis,"gbk");
            configPro.load(fr);
            fr.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public boolean setConfigFile(String fileName) {
        this.configFile = fileName;
        if (fileName == null)
            return false;
        try {
            // FileReader fr = new FileReader(this.configFile);
            InputStreamReader fr= new InputStreamReader(new FileInputStream(this.configFile),"gbk");
            configPro.load(fr);
            fr.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean setSourceFile(String fileName) {
        this.sourceFile = fileName;
        if (fileName == null)
            return false;
        if (br != null) {
            try {
                br.close();
                br = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            //  br = new BufferedReader(new FileReader(this.sourceFile), 1024);
            // br = new InputStreamReader(new FileReader(this.sourceFile), 1024);
            inRead = new InputStreamReader(new FileInputStream(this.sourceFile),"gbk");
            br=new BufferedReader(inRead);
            currentAttList.clear();
            currentDataLine = null;
            currentTag = null;
            isRealEnd = false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean setSourceFile(InputStream fis) {
        if (fis == null)
            return false;
        if (br != null) {
            try {
                br.close();
                br = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            //  br = new BufferedReader(new FileReader(this.sourceFile), 1024);
            // br = new InputStreamReader(new FileReader(this.sourceFile), 1024);
            inRead = new InputStreamReader(fis,"gbk");
            br=new BufferedReader(inRead);
            currentAttList.clear();
            currentDataLine = null;
            currentTag = null;
            isRealEnd = false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String getCurrentTag() {
        if (currentTag != null)
            return currentTag;
        String curLine = null;
        if (br == null)
            return null;
        try {
            curLine = br.readLine();
            currentTag = matchTagStarter(curLine);
            while (currentTag == null) {
                curLine = br.readLine();
                if (curLine == null) {
//					log.info("end of file without any tag found");
                    isRealEnd = true;
                    br.close();
                    inRead.close();
                    br = null;
                    return null;
                }
                currentTag = matchTagStarter(curLine);
            }
            curLine = br.readLine();
            while (!matchAttributeNameLine(curLine)) {
                curLine = br.readLine();
            }
            if (configPro == null)
                return null;
            String attrbreaker = configPro.getProperty(ATTRIBUTEBREAKER);
            StringTokenizer st = null;
            if (attrbreaker.trim().equals("")) {
                st = new StringTokenizer(curLine, " ");
            } else {
                st = new StringTokenizer(curLine, attrbreaker);
            }
            currentAttList.clear();
            st.nextToken();
            while (st.hasMoreTokens()) {
                currentAttList.add(st.nextToken().trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return currentTag;
    }

    public List<String> getCurrentTagAtrributeName() {
        if (currentTag == null)
            return null;
        return currentAttList;
    }

    public String readToNextTag() {
        currentTag = null;
        return getCurrentTag();
    }

    public HashMap<String, String> readNextDataMap() {
        HashMap<String, String> dataMap = new HashMap<String, String>();
        String dataLine = readNextDataLine();
        if (dataLine == null)
            return null;
        if (configPro == null)
            return null;
        String dataBreaker = configPro.getProperty(DATABREAKER);
        StringTokenizer st = null;
        if (dataBreaker.trim().equals("")) {
            st = new StringTokenizer(dataLine, " ");
        } else {
            st = new StringTokenizer(dataLine, dataBreaker);
        }
        st.nextToken();
        int i = 0;
        while (st.hasMoreTokens()) {
            // modified by Liying, 2012-4-11
            String value = st.nextToken().trim();
            if (value == null || value.equalsIgnoreCase("null")) {
                i++;
                continue;
            }
            // modification end
            //System.out.println("i = "+i+" key= "+currentAttList.get(i)+" value = "+value);
            dataMap.put(currentAttList.get(i), value);
            i++;
        }
        return dataMap;
    }

    public String readNextDataLine() {
        if (currentTag == null)
            return null;
        if (br == null)
            return null;
        currentDataLine = null;
        try {
            String currentLine = br.readLine();
            while (!matchDataLine(currentLine) && !matchTagEnder(currentLine)) {
//				log.info("no data line no end tag" + currentLine);
                if (currentLine == null) {
//					log.info("end of file without any data line found");
                    br.close();
                    br = null;
                    isRealEnd = false;
                    return null;
                }
                currentLine = br.readLine();
            }
            if (matchTagEnder(currentLine))
                return null;
            currentDataLine = currentLine;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return currentDataLine;
    }

    public boolean isRealEnd() {
        return isRealEnd;
    }

    public String getCurrentDataLine() {
        return currentDataLine;
    }

    public String matchTagStarter(String str) {
        if (configPro == null)
            return null;
        if (str == null)
            return null;
        String tagStarter = configPro.getProperty(TAGSTARTER);
        String tagEnder = configPro.getProperty(TAGENDER);
        String tagStartChar = tagStarter.substring(0, tagStarter.indexOf("xx"));
        String tagEndChar = tagEnder.substring(0, tagEnder.indexOf("xx"));
        if (str.startsWith(tagStartChar) && !str.startsWith(tagEndChar)) {
            int idxOfBlank = str.indexOf(' ');
            int idxOfEndChar = str.indexOf(tagStarter.charAt(tagStarter.length() - 1));
            if (idxOfBlank == -1 && idxOfEndChar == -1)
                return null;
            if (idxOfBlank == -1)
                return str.substring(tagStartChar.length(), idxOfEndChar);
            return str.substring(tagStartChar.length(), idxOfBlank);
        }
        return null;
    }

    public boolean matchTagEnder(String str) {
        if (configPro == null)
            return false;
        if (str == null)
            return false;
        String tagEnder = configPro.getProperty(TAGENDER);
        String tagEndChar = tagEnder.substring(0, tagEnder.indexOf("xx"));
        if (str.startsWith(tagEndChar)) {
            return true;
        }
        return false;
    }

    public boolean matchAttributeNameLine(String str) {
        if (configPro == null)
            return false;
        if (str == null)
            return false;
        String attStarter = configPro.getProperty(ATTRIBUTENAMESTARTER);
        return str.startsWith(attStarter);
    }

    public boolean matchCommentNameLine(String str) {
        if (configPro == null)
            return false;
        if (str == null)
            return false;
        String commStarter = configPro.getProperty(COMMENTSTARTER);
        return str.startsWith(commStarter);
    }

    public boolean matchDataLine(String str) {
        if (configPro == null)
            return false;
        if (str == null)
            return false;
        String dataStarter = configPro.getProperty(DATALINESTARTER);
        return str.startsWith(dataStarter);
    }
    public boolean isQS() {
        boolean isQS = false;
        String curLine = null;
        try {
            if (br == null)
                return false;
            curLine = br.readLine();
            if (curLine == null)
                return false;
            if (curLine.startsWith("<!") && curLine.endsWith("!>")) {
                while (curLine.contains("=")) {
                    int index1 = curLine.indexOf(" ");
                    int index2 = curLine.indexOf("=");
                    String key = curLine.substring(index1 + 1, index2);
                    curLine = curLine.substring(index2);
                    int index3 = curLine.indexOf(" ");
                    String value = curLine.substring(1, index3);
                    curLine = curLine.substring(index3);
                    key=key.trim();
                    value=value.trim();
                    qsProp.put(key, value);
                }
                isQS = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isQS;
    }

    public Map<String, String> readQSProperty() {
        return qsProp;
    }


    public void parseQSfile()
    {
        configPro=getConfigPro();
        Properties prop = getConfigPro();
        String defaultId = prop.getProperty("storeid");
        if (defaultId == null)
            defaultId = "唯一标志"; // default id word
        //若有多个QS文件没有Id属性，则都会保存成“唯一标志”
        dataMap = new HashMap<String, HashMap<String, Object>>();
        //读入元数据
        Map<String, String> qsProp = new HashMap<String, String>();
        HashMap<String,Object> qsMap=new HashMap<String,Object>();
        if (isQS()) {
            qsProp = readQSProperty();
//            Set<String> qsPropKeys = qsProp.keySet();
//            for (String qsProsKey : qsPropKeys) {
//                metadata.setDataObj(qsProsKey, "QS", qsProp.get(qsProsKey));
//            }
            qsMap.put("1",qsProp);
            dataMap.put("QS",qsMap);
        }
        String tag=getCurrentTag();
        //qsReader.getCurrentTagAtrributeName();
        while(tag!=null)
        {

            System.out.println("Tag= "+tag);
            HashMap<String, String> data = readNextDataMap();
            String tagId = prop.getProperty(tag + ".storeid");
            if (tagId == null)
                tagId = defaultId;
            String[] idArray = tagId.split(";");
            HashMap<String, Object> idMap = new HashMap<String, Object>();
            while (data != null)
            {
                String objKey = "";
                for (String id : idArray) {
                    objKey += data.get(id);
                }
                if (!objKey.equals("")) {
                    idMap.put(objKey,data);
                }
                data = readNextDataMap();
            }
            dataMap.put(tag,idMap);
            tag = readToNextTag();
        }
    }


    public  HashMap<String, HashMap<String, Object>> getDataMap()
    {
        return dataMap;
    }

    public void setDataMap( HashMap<String, HashMap<String, Object>>  map){
        dataMap=map;
    }


    void printMap( HashMap<String, Object> dataMap){
        System.out.println("=========================");

        int nRecords=dataMap.keySet().size();
        if(nRecords<1)
            return;
        int i=0;
        HashMap<String,String> objmap=null;
        for(String str:dataMap.keySet())
        {

            objmap=(HashMap<String,String>)dataMap.get(str);
            if(i==0)
            {
                System.out.print("#");
                for(String props : objmap.keySet())
                    System.out.print(props+" \t ");
                System.out.print("\n");
            }

            System.out.print(str+"  ---> \n");
            for(String props : objmap.keySet())
            {
                if(props.equalsIgnoreCase("FinalVolt")||props.equalsIgnoreCase("FinalAng")||props.equalsIgnoreCase("Id"))
                    continue;
                System.out.print("\t "+props+"   "+ objmap.get(props)+" \n ");
            }
            System.out.print("\n");
            i++;
        }
        System.out.println("=========================");
    }

    public static void main(String [] args)
    {

        FileReader4EFormat myreader = new FileReader4EFormat();
        //myreader.setConfigFile("E://QSconfig.properties");
        //myreader.setSourceFile("E://cime-v7.txt");
        myreader.setConfigFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/QSconfig.properties"));
        myreader.setSourceFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/CIME-IEEE39.txt"));

        myreader.parseQSfile();

        myreader.printMap(myreader.getDataMap().get("Aclinesegment"));
        //myreader.printMap(myreader.getDataMap().get("Disconnector"));

        HashMap<String, HashMap<String, Object>> dataMap=myreader.getDataMap();
        //String Tag="Breaker";
        //String Tag="Disconnector";
        String Tag="basePower";
        HashMap<String,Object>tagMap=dataMap.get(Tag);
        HashMap<String,String> objmap=null;
        for(String str:tagMap.keySet())
        {
            objmap=(HashMap<String,String>)tagMap.get(str);

            for(String props : objmap.keySet())
            {
                if(props.equalsIgnoreCase("FinalVolt")||props.equalsIgnoreCase("FinalAng")||props.equalsIgnoreCase("Id"))
                    continue;
                System.out.print(props+" = "+ objmap.get(props)+" \t ");
            }
            System.out.println();

        }
        return;
    }
}
