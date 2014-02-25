import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {

    private Properties properties;
    private static volatile Config instance;

    public static Config getInstance() {
        Config localInstance = instance;
        if (localInstance == null) {
            synchronized (Config.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new Config();
                }
            }
        }
        return localInstance;
    }

    public Config () {
        properties = new Properties();
        try {
            //load a properties file
            properties.load(new FileInputStream("config.properties"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String Get(String param)
    {
        return properties.getProperty(param);
    }

    public void Set(String param, String value)
    {
        Set(param, value, Boolean.FALSE);
    }

    public void Set(String param, String value, Boolean store)
    {
        properties.setProperty(param, value);

        if (store)
        {
            try {
                properties.store(new FileOutputStream("config.properties"), null);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}