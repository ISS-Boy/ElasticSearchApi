package test.com.iss.bigdata.health.elasticsearch.service;

import com.iss.bigdata.health.elasticsearch.entity.*;
import com.iss.bigdata.health.elasticsearch.help.EventMap;
import com.iss.bigdata.health.elasticsearch.help.QueryObject;
import com.iss.bigdata.health.elasticsearch.service.ElasticSearchService;
import com.iss.bigdata.health.elasticsearch.service.ElasticSearchServiceImpl;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

/**
 * Created by dujijun on 2018/1/3.
 */
public class ServiceTest {
    ElasticSearchService service;
    Date start;
    Date end;

    @Before
    public void prepareClient(){
        System.setProperty("user.timezone", "GMT");

        service = new ElasticSearchServiceImpl();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 100);
        start = calendar.getTime();
        calendar.set(Calendar.YEAR, 3000);
        end = calendar.getTime();

    }

    @Test
    public void getUserBasicTest() throws IOException {
        Event<UserBasic> userBasic = service.getUserBasicByUserId("the-user-6706");
        System.out.println(userBasic);
    }

    @Test
    public void getCarePlanTest() throws IOException {
        List<Event<CarePlan>> carePlans = service.getCarePlanEventsByUserId("the-user-8510", start, end);
        System.out.println(carePlans);
    }


    @Test
    public void getImmunizationTest() throws IOException {
        List<Event<Immunization>> immunizations = service.getImmunizationEventsByUserId("the-user-87", start, end);
        immunizations.forEach(System.out::println);
        System.out.println(immunizations.size());
    }

    @Test
    public void getEncounterTest() throws IOException {
        List<Event<Encounter>> encounters = service.getEncounterEventsByUserId("the-user-87", start, end);
        encounters.forEach(System.out::println);
        System.out.println(encounters.size());
    }

    @Test
    public void eventMapTest() throws IOException {
        List<Event<Observation>> observations = service.getObservationEventsByUserId("the-user-87", start, end);
        List<Event<Allergy>> allergies = service.getAllergyEventsByUserId("the-user-38", start, end);

        EventMap map = new EventMap();
        map.put("allergies", allergies);
        map.put("observation", observations);

        System.out.println(map.getEventList(
                EventMap.getEventClassFromEnum(EventMap.EventType.ALLERGY),
                                        "allergies").size());

        System.out.println(map.getEventList(EventMap.getEventClassFromString("observations"), "observations"));
    }

    @Test
    public void queryObjectTest(){
        List<QueryObject> list = new ArrayList<>();
        QueryObject<Allergy> allergyQueryObject = new QueryObject<>();
        list.add(allergyQueryObject);
        list.get(0);
    }

    @Test
    public void getMultiTypeEventsByUserIdTest() throws IOException {
        List<QueryObject> queries = new ArrayList<>();

        queries.add(new QueryObject("the-user-87", "conditions", "synthea", Condition.class, start, end));
        queries.add(new QueryObject("the-user-87", "careplans", "synthea", CarePlan.class, start, end));

        EventMap eventMap = service.getMultiTypeEventsByUserId(queries, "start", SortOrder.DESC);
        System.out.println(eventMap);

    }

    @Test
    public void getAllEventsByUserIdTest() throws IOException {
        EventMap allEvent = service.getAllTypeEventByUserId("the-user-38", start, end);
        allEvent.forEach((k, v) -> System.out.printf("%s类型的事件中有%d条记录\n", k, v.size()));
    }

    @Test
    public void getEventDateTest() throws IOException {
        EventMap allEvent = service.getAllTypeEventByUserId("the-user-38", start, end);
        allEvent.forEach((k, v) -> {
            System.out.println(k + "=============");
            v.forEach(e -> System.out.println(((Event)e).getDate()));
        });
    }

    @Test
    public void getMultiTypeEventsByUserId(){
        List<QueryObject> queryObjects = new ArrayList<>();
        queryObjects.add(new QueryObject("the-user-38", "allergies", "synthea", Allergy.class, start, end));
        queryObjects.add(new QueryObject("the-user-38", "immunizations", "synthea", Immunization.class, start, end));
        EventMap allEvent = service.getSeveralTypeEventsByUserId(queryObjects);
        allEvent.forEach((k, v) -> System.out.printf("%s类型的事件中有%d条记录\n", k, v.size()));
    }

}
