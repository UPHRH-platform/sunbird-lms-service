/**
 * 
 */
package controllers.healthmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.request.Request;

import akka.util.Timeout;
import controllers.BaseController;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * @author Manzarul
 */
public class HealthController extends BaseController {
  private static List<String> list = new ArrayList<>();
  
  static {
    list.add("learner");
    list.add("actor");
    list.add("cassandra");
    list.add("es");
    list.add("ekstep");
  }
  
  /**
   * This method will do the complete health check
   * @return Promise<Result>
   */
  public Promise<Result> getHealth() {
    try {
      ProjectLogger.log("Call to get all server health api = " , LoggerEnum.INFO.name());
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.HEALTH_CHECK.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.getRequest().put(JsonKey.CREATED_BY,
          getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
      reqObj.setEnv(getEnvironment());
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      return actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
  
  
  /**
   * This method will do the health check for play service.
   * @return Promise<Result>
   */
  public Promise<Result> getLearnerServiceHealth(String val) {
    ProjectLogger.log("Call to get play service health api = " + val , LoggerEnum.INFO.name());
    Map<String,Object> finalResponseMap = new HashMap<>();
    List<Map<String,Object>> responseList  =  new ArrayList<> ();
      if(list.contains(val) && !"learner".equalsIgnoreCase(val)) {
         if(ActorOperations.EKSTEP.name().equalsIgnoreCase(val)) {
          return getEkStepHealtCheck(request());
         } else{ 
        try{
        Request reqObj = new Request();
        reqObj.setOperation(val);
        reqObj.setRequest_id(ExecutionContext.getRequestId());
        reqObj.getRequest().put(JsonKey.CREATED_BY,
            getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
        reqObj.setEnv(getEnvironment());
        Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
        return actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
      } catch (Exception e) {
        return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
      }
      }
      }else {
      responseList.add(ProjectUtil.createCheckResponse(JsonKey.LEARNER_SERVICE, false, null));
      finalResponseMap.put(JsonKey.CHECKS, responseList);
      finalResponseMap.put(JsonKey.NAME, "Learner service health");
      finalResponseMap.put(JsonKey.Healthy, true);
      Response response = new  Response();
      response.getResult().put(JsonKey.RESPONSE, finalResponseMap);
      response.setId("learner.service.health.api");
      response.setVer(getApiVersion(request().path()));
      response.setTs(ExecutionContext.getRequestId());
      return Promise.<Result>pure(ok(play.libs.Json.toJson(response)));
     }
  }
  
  
  public Promise<Result> getEkStepHealtCheck(play.mvc.Http.Request request) {
    Map<String, Object> finalResponseMap = new HashMap<>();
    List<Map<String, Object>> responseList = new ArrayList<>();
    // check EKStep Util.
    try {
      ProjectUtil.registertag("testtag1233", "{}",
          getAllRequestHeaders(request));
      responseList.add(
          ProjectUtil.createCheckResponse(JsonKey.EKSTEP_SERVICE, false, null));
      finalResponseMap.put(JsonKey.Healthy, true);
    } catch (Exception e) {
      responseList.add(
          ProjectUtil.createCheckResponse(JsonKey.EKSTEP_SERVICE, true, null));
      finalResponseMap.put(JsonKey.Healthy, false);
    }
    finalResponseMap.put(JsonKey.CHECKS, responseList);
    finalResponseMap.put(JsonKey.NAME, "EkStep service health");
    Response response = new Response();
    response.getResult().put(JsonKey.RESPONSE, finalResponseMap);
    response.setId("Ekstep.service.health.api");
    response.setVer(getApiVersion(request().path()));
    response.setTs(ExecutionContext.getRequestId());
    return Promise.<Result>pure(ok(play.libs.Json.toJson(response)));
  }
  
  
  /**
   * 
   * @param request
   * @return Map<String, String>
   */
  private Map<String, String> getAllRequestHeaders(play.mvc.Http.Request request) {
    Map<String, String> map = new HashMap<>();
    Map<String, String[]> headers = request.headers();
    Iterator<Entry<String, String[]>> itr = headers.entrySet().iterator();
    while (itr.hasNext()) {
      Entry<String, String[]> entry = itr.next();
      map.put(entry.getKey(), entry.getValue()[0]);
    }
    return map;
  }
}
