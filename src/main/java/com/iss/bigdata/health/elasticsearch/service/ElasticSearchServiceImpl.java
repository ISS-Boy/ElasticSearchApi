package com.iss.bigdata.health.elasticsearch.service;

import com.alibaba.fastjson.JSON;
import com.iss.bigdata.health.elasticsearch.entity.*;
import com.iss.bigdata.health.elasticsearch.help.EventMap;
import com.iss.bigdata.health.elasticsearch.help.QueryObject;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dujijun on 2018/1/3.
 */
public class ElasticSearchServiceImpl implements ElasticSearchService {
    private RestHighLevelClient client;
    private static final int MAX_QUERY_SIZE = 10000;

    public ElasticSearchServiceImpl() {
        this.client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")
                )
        );
    }

    
    @Override
    public EventMap getAllTypeEventByUserId(String userId, Date start, Date end){
        List<QueryObject> startQueries = new ArrayList<>();
        startQueries.add(new QueryObject(userId, "conditions", "synthea", Condition.class, start, end));
        startQueries.add(new QueryObject(userId, "allergies", "synthea", Allergy.class, start, end));
        startQueries.add(new QueryObject(userId, "careplans", "synthea", CarePlan.class, start, end));
        startQueries.add(new QueryObject(userId, "medications", "synthea", Medication.class, start, end));

        List<QueryObject> dateQueries = new ArrayList<>();
        dateQueries.add(new QueryObject(userId, "observation", "synthea", Observation.class, start, end));
        dateQueries.add(new QueryObject(userId, "immunizations", "synthea", Immunization.class, start, end));
        dateQueries.add(new QueryObject(userId, "encounters", "synthea", Encounter.class, start, end));

        EventMap events = new EventMap();
        events.putAll(getMutiTypeEventsByUserIdOrderByDate(dateQueries));
        events.putAll(getMutiTypeEventsByUserIdOrderByStart(startQueries));
        return events;
    }

    /**
     * 组合查询, 查询多个类型的EventList
     * @param queryRequests 查询请求列表
     * @return 多事件映射
     */
    @Override
    public EventMap getSeveralTypeEventsByUserId(List<QueryObject> queryRequests){
        Map<String, List<QueryObject>> queryGroup = queryRequests
                                                        .stream()
                                                        .collect(Collectors.groupingBy(q -> q.getFilterNameAndOrderKey()));
        EventMap resultMap = new EventMap();
        resultMap.putAll(getMutiTypeEventsByUserIdOrderByDate(queryGroup.get("date")));
        resultMap.putAll(getMutiTypeEventsByUserIdOrderByStart(queryGroup.get("start")));
        return resultMap;
    }

    /**
     * 获取通过事件发生日期排序的类型的事件
     */
    
    @Override
    public EventMap getMutiTypeEventsByUserIdOrderByDate(List<QueryObject> queryRequests){
        return getMultiTypeEventsByUserId(queryRequests, "date", SortOrder.DESC);
    }

    /**
     * 获取通过事件起始日期排序的类型的事件
     */
    
    @Override
    public EventMap getMutiTypeEventsByUserIdOrderByStart(List<QueryObject> queryRequests){
        return getMultiTypeEventsByUserId(queryRequests, "start", SortOrder.DESC);
    }

    /**
     * 多类型查询, 通过查询对象传入需要的查询与排序键, 得到所有的查询结果
     * 注意：传入的List中的查询对象的排序键和参数中的排序键必须相同，否则会抛出异常
     * 推荐使用
     *  {@link ElasticSearchServiceImpl#getMutiTypeEventsByUserIdOrderByDate(List)}
     *  {@link ElasticSearchServiceImpl#getMutiTypeEventsByUserIdOrderByStart(List)}
     *  {@link ElasticSearchServiceImpl#getMultiTypeEventsByUserId(List, String, SortOrder)}
     * @param queryRequests 查询请求对象
     * @param orderKey 排序键
     * @param order 排序方式
     * @return 事件映射列表
     * @throws IOException
     */
    
    @Override
    public EventMap getMultiTypeEventsByUserId(List<QueryObject> queryRequests, String orderKey, SortOrder order){
        EventMap eventMap = new EventMap();
        SearchRequest searchRequest = new SearchRequest();
        List<String> indice = new ArrayList<>();

        // 构建EventMap
        buildEventMap(eventMap, queryRequests);

        // 构建搜索语句
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 构建每一个Query的查询
        for (QueryObject queryRequest : queryRequests) {
            // 当排序键不一致时，抛出异常
            if(!queryRequest.getFilterNameAndOrderKey().equals(orderKey))
                throw new RuntimeException("使用错误，请保证参数中的所有排序键和查询对象中的排序键一直");

            indice.add(queryRequest.getIndexName());
            searchRequest.types(queryRequest.getType());

            boolQueryBuilder.should(QueryBuilders
                    .boolQuery()
                    .must(QueryBuilders.termQuery("_index", queryRequest.getIndexName()))
                    .must(QueryBuilders.termQuery("user_id.keyword", queryRequest.getUserId()))
                    .must(QueryBuilders.rangeQuery(queryRequest.getFilterNameAndOrderKey())
                                                                .from(queryRequest.getStart())
                                                                .to(queryRequest.getEnd())));

        }
        // 设置查询索引
        String[] indiceArr = new String[indice.size()];
        indice.toArray(indiceArr);
        searchRequest.indices(indiceArr);

        // 设置结果集排序
        sourceBuilder.sort(orderKey, order);
        sourceBuilder.size(MAX_QUERY_SIZE);
        sourceBuilder.query(boolQueryBuilder);

        // 打印查询语句
//        System.out.println(sourceBuilder.query());

        searchRequest.source(sourceBuilder);


        // 获取输出结果
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("连接或查询有误, 请检查您的网络连接是否畅通，并检查查询项是否有误!");
        }
        for(SearchHit hit : searchResponse.getHits()){
            putEachEventIntoList(eventMap, hit, orderKey);
        }

        return eventMap;
    }

    // 构建对应事件信息列表
    private void putEachEventIntoList(EventMap eventMap, SearchHit hit, String orderKey) {
        Date date = JSON.parseObject(hit.getSourceAsString()).getDate(orderKey);
        switch (hit.getIndex()) {
            case "allergies":
                eventMap.getEventList(Allergy.class, "allergies")
                        .add(new Event<>(hit.getId(),
                                         JSON.parseObject(hit.getSourceAsString(),
                                                 Allergy.class), date));
                break;
            case "encounters":
                eventMap.getEventList(Encounter.class, "encounters")
                        .add(new Event<>(hit.getId(),
                                JSON.parseObject(hit.getSourceAsString(),
                                        Encounter.class), date));
                break;
            case "conditions":
                eventMap.getEventList(Condition.class, "conditions")
                        .add(new Event<>(hit.getId(),
                                JSON.parseObject(hit.getSourceAsString(),
                                        Condition.class), date));
                break;
            case "medications":
                eventMap.getEventList(Medication.class, "medications")
                        .add(new Event<>(hit.getId(),
                                JSON.parseObject(hit.getSourceAsString(),
                                        Medication.class), date));
                break;
            case "immunizations":
                eventMap.getEventList(Immunization.class, "immunizations")
                        .add(new Event<>(hit.getId(),
                                JSON.parseObject(hit.getSourceAsString(),
                                        Immunization.class), date));
                break;
            case "observation":
                eventMap.getEventList(Observation.class, "observation")
                        .add(new Event<>(hit.getId(),
                                JSON.parseObject(hit.getSourceAsString(),
                                        Observation.class), date));
                break;
            case "careplans":
                eventMap.getEventList(CarePlan.class, "careplans")
                        .add(new Event<>(hit.getId(),
                                JSON.parseObject(hit.getSourceAsString(),
                                        CarePlan.class), date));
                break;

        }
    }

    private void buildEventMap(EventMap map, List<QueryObject> queryRequests){
        for(QueryObject q : queryRequests)
            map.put(q.getIndexName(), createResultList(q.getClassType()));
    }

    private <T> List<T> createResultList(Class<T> classType){
        return new ArrayList<>();
    }

    /**
     * 不排序的EventList
     * @param userId 用户的id
     * @param indexName 索引名——即事件名
     * @param type 索引类型，一般统一为synthea
     * @param classType 需要获取事件实体的Class的类型
     * @return 事件列表
     * @throws IOException
     */
    
    @Override
    public <T> List<Event<T>> getSpecificEventsByUserId(String userId, String indexName, String type, Class<T> classType){
        return getSpecificEventsByUserId(userId,
                                 indexName,
                                 type,
                                 classType,
                                 new HashMap<>(),
                                 "timestamp",
                                 null,
                                 null
                );
    }

    /**
     * 单类型查询, 通过UserId来获取相应的单类型事件列表
     * @param userId 用户的id
     * @param indexName 索引名——即事件名
     * @param type 索引类型，一般统一为synthea
     * @param classType 需要获取事件实体的Class的类型
     * @param orderFields Map<String, SortOrder> (排序列名, 顺序)
     * @param filterName 过滤域名称
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 事件列表
     */
    
    @Override
    public <T> List<Event<T>> getSpecificEventsByUserId(String userId,
                                                        String indexName,
                                                        String type,
                                                        Class<T> classType,
                                                        Map<String, SortOrder> orderFields,
                                                        String filterName,
                                                        Date startDate,
                                                        Date endDate){
        List<Event<T>> events = new ArrayList<>();

        SearchRequest searchRequest = new SearchRequest(indexName);
        searchRequest.types(type);

        // 构建搜索语句
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // 使用bool组合查询
        sourceBuilder.query(QueryBuilders.boolQuery()
                                        .must(QueryBuilders.termQuery("user_id.keyword", userId))
                                        .must(QueryBuilders.rangeQuery(filterName).from(startDate).to(endDate)));
        // 按照所选字段和顺序进行排序
        orderFields.forEach(sourceBuilder::sort);
        sourceBuilder.size(MAX_QUERY_SIZE);

        // 发送搜索请求并分析获取结果
        searchRequest.source(sourceBuilder);
//        searchRequest.scroll(TimeValue.timeValueSeconds(30));
        SearchResponse result = null;
        try {
            result = client.search(searchRequest);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("连接或查询有误, 请检查您的网络连接是否畅通，并检查查询项是否有误!");
        }
        SearchHits hits = result.getHits();
        for (SearchHit hit : hits.getHits()){
            T t = JSON.parseObject(hit.getSourceAsString(), classType);
            Date date = JSON.parseObject(hit.getSourceAsString()).getDate(filterName);
            events.add(new Event<>(hit.getId(), t, date));
        }
        return events;
    }

    /**
     * 获取encounter事件，按照date来进行排序
     * @return 事件列表
     * @throws IOException
     */
    
    @Override
    public List<Event<Encounter>> getEncounterEventsByUserId(String userId, Date startDate, Date endDate){
        HashMap<String, SortOrder> fieldOrder = new HashMap<>();
        fieldOrder.put("date", SortOrder.DESC);
        return getSpecificEventsByUserId(userId,
                       "encounters",
                            "synthea",
                                 Encounter.class,
                                 fieldOrder,
                                 "date",
                                 startDate, endDate
                                 );
    }

    /**
     * immunizations，按照date来进行排序
     * @return 事件列表
     * @throws IOException
     */
    
    @Override
    public List<Event<Immunization>> getImmunizationEventsByUserId(String userId, Date startDate, Date endDate){
        HashMap<String, SortOrder> fieldOrder = new HashMap<>();
        fieldOrder.put("date", SortOrder.DESC);
        return getSpecificEventsByUserId(userId,
                "immunizations",
                "synthea",
                Immunization.class,
                fieldOrder,
                "date",
                startDate, endDate
        );
    }

    /**
     * 获取observations事件，按照date来进行排序
     * @return 事件列表
     * @throws IOException
     */
    
    @Override
    public List<Event<Observation>> getObservationEventsByUserId(String userId, Date startDate, Date endDate){
        HashMap<String, SortOrder> fieldOrder = new HashMap<>();
        fieldOrder.put("date", SortOrder.DESC);
        return getSpecificEventsByUserId(userId,
                "observation",
                "synthea",
                Observation.class,
                fieldOrder,
                "date",
                startDate, endDate

        );
    }

    /**
     * 获取medication事件，按照start(事件开始事件)来进行排序
     * @return 事件列表
     * @throws IOException
     */
    
    @Override
    public List<Event<Medication>> getMedicationEventsByUserId(String userId, Date startDate, Date endDate){
        HashMap<String, SortOrder> fieldOrder = new HashMap<>();
        fieldOrder.put("start", SortOrder.DESC);
        return getSpecificEventsByUserId(userId,
                "medications",
                "synthea",
                Medication.class,
                fieldOrder,
                "start",
                startDate, endDate
        );
    }

    /**
     * 获取careplans事件，按照start来进行排序
     * @return 事件列表
     * @throws IOException
     */
    
    @Override
    public List<Event<CarePlan>> getCarePlanEventsByUserId(String userId, Date startDate, Date endDate){
        HashMap<String, SortOrder> fieldOrder = new HashMap<>();
        fieldOrder.put("start", SortOrder.DESC);
        return getSpecificEventsByUserId(userId,
                "careplans",
                "synthea",
                CarePlan.class,
                fieldOrder,
                "start",
                startDate, endDate
        );
    }

    /**
     * 获取allergies事件，按照start(事件开始事件)来进行排序
     * @return 事件列表
     * @throws IOException
     */
    
    @Override
    public List<Event<Allergy>> getAllergyEventsByUserId(String userId, Date startDate, Date endDate){
        HashMap<String, SortOrder> fieldOrder = new HashMap<>();
        fieldOrder.put("start", SortOrder.DESC);
        return getSpecificEventsByUserId(userId,
                "allergies",
                "synthea",
                Allergy.class,
                fieldOrder,
                "start",
                startDate, endDate
        );
    }

    /**
     * 获取conditions事件，按照start(事件开始事件)来进行排序
     * @return 事件列表
     * @throws IOException
     */
    
    @Override
    public List<Event<Condition>> getConditionEventsByUserId(String userId, Date startDate, Date endDate){
        HashMap<String, SortOrder> fieldOrder = new HashMap<>();
        fieldOrder.put("start", SortOrder.DESC);
        return getSpecificEventsByUserId(userId,
                "conditions",
                "synthea",
                Condition.class,
                fieldOrder,
                "start",
                startDate, endDate
        );
    }

    /**
     * 通过UserId来获取用户数据，由于对于每个User来说这个Event只会有唯一的一个，故获取其第一个元素即可
     * @param userId
     * @return 事件列表
     * @throws IOException
     */
    
    @Override
    public Event<UserBasic> getUserBasicByUserId(String userId){
        List<Event<UserBasic>> userBasicList = getSpecificEventsByUserId(userId, "patient", "synthea", UserBasic.class);
        return userBasicList.size() == 0 ? null : userBasicList.get(0);
    }

}
