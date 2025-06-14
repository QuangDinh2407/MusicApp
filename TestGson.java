import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.ArrayList;

public class TestGson {
    public void testGson() {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<String>>(){}.getType();
        
        List<String> testList = new ArrayList<>();
        testList.add("test");
        
        String json = gson.toJson(testList);
        List<String> fromJson = gson.fromJson(json, listType);
        
        System.out.println("Gson works: " + fromJson.get(0));
    }
} 