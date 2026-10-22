import com.google.gson.*;
import com.safarparmar.app.feature.habits.data.HabitBatchRequest;
import com.safarparmar.app.feature.habits.data.HabitBatchResponse;
import java.util.*;
public class HabitWireProbe {
 public static void main(String[] args) {
  Gson gson=new GsonBuilder().serializeNulls().create();
  JsonObject mutation=new JsonObject();
  mutation.addProperty("mutationId","test-mutation");
  mutation.addProperty("habitId","test-habit");
  mutation.addProperty("type","create");
  mutation.addProperty("name","Read");
  mutation.add("reminderTime",JsonNull.INSTANCE);
  String request=gson.toJson(new HabitBatchRequest(Collections.singletonList(mutation)));
  System.out.println("request="+request);
  System.out.println("requestKeyPreserved="+JsonParser.parseString(request).getAsJsonObject().has("mutations"));
  HabitBatchResponse response=gson.fromJson("{\"version\":3,\"acknowledged\":[\"test-mutation\"]}",HabitBatchResponse.class);
  System.out.println("responseVersion="+response.getVersion());
  System.out.println("responseAcknowledged="+response.getAcknowledged());
  boolean success=JsonParser.parseString(request).getAsJsonObject().has("mutations") && response.getVersion()==3 && Collections.singletonList("test-mutation").equals(response.getAcknowledged());
  System.out.println("contract="+(success?"PASS":"FAIL"));
  if (args.length>0 && args[0].equals("require-pass") && !success) throw new AssertionError("Habit request/response contract broken after minification");
 }
}
