import com.ck.music_app.utils.VoiceSearchHelper;
import com.ck.music_app.utils.PersonalizedSearchHelper;
import com.ck.music_app.utils.EmotionGenreSearchHelper;
import com.ck.music_app.utils.SearchUtils;

public class TestVoiceSearch {
    public void testMethods() {
        // Test VoiceSearchHelper methods exist
        VoiceSearchHelper helper = null;
        if (helper != null) {
            helper.checkPermission();
            helper.startListening();
            helper.stopListening();
            helper.isListening();
            helper.destroy();
            // helper.requestPermission(activity); // This needs Activity parameter
        }
        
        // Test other classes
        PersonalizedSearchHelper personalizedHelper = null;
        EmotionGenreSearchHelper emotionHelper = null;
        SearchUtils searchUtils = null;
        
        System.out.println("All imports and methods are accessible!");
    }
} 