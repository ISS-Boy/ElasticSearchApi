package com.iss.bigdata.health.elasticsearch.service;

import com.iss.bigdata.health.elasticsearch.entity.*;
import com.iss.bigdata.health.elasticsearch.help.EventMap;
import com.iss.bigdata.health.elasticsearch.help.QueryObject;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by dujijun on 2018/1/5.
 */
public interface ElasticSearchService {
    EventMap getAllTypeEventByUserId(String userId, Date start, Date end);

    /**
     * 组合查询, 查询多个类型的EventList
     * @param queryRequests 查询请求列表
     * @return 多事件映射
     */
    EventMap getSeveralTypeEventsByUserId(List<QueryObject> queryRequests);

    /**
     * 获取通过事件发生日期排序的类型的事件
     */

    EventMap getMutiTypeEventsByUserIdOrderByDate(List<QueryObject> queryRequests);

    /**
     * 获取通过事件起始日期排序的类型的事件
     */

    EventMap getMutiTypeEventsByUserIdOrderByStart(List<QueryObject> queryRequests);

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

    EventMap getMultiTypeEventsByUserId(List<QueryObject> queryRequests, String orderKey, SortOrder order);

    /**
     * 不排序的EventList
     * @param userId 用户的id
     * @param indexName 索引名——即事件名
     * @param type 索引类型，一般统一为synthea
     * @param classType 需要获取事件实体的Class的类型
     * @return 事件列表
     * @throws IOException
     */

    <T> List<Event<T>> getSpecificEventsByUserId(String userId, String indexName, String type, Class<T> classType);

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

    <T> List<Event<T>> getSpecificEventsByUserId(String userId,
                                                 String indexName,
                                                 String type,
                                                 Class<T> classType,
                                                 Map<String, SortOrder> orderFields,
                                                 String filterName,
                                                 Date startDate,
                                                 Date endDate);

    /**
     * 获取encounter事件，按照date来进行排序
     * @return 事件列表
     * @throws IOException
     */

    List<Event<Encounter>> getEncounterEventsByUserId(String userId, Date startDate, Date endDate);

    /**
     * immunizations，按照date来进行排序
     * @return 事件列表
     * @throws IOException
     */

    List<Event<Immunization>> getImmunizationEventsByUserId(String userId, Date startDate, Date endDate);

    /**
     * 获取observations事件，按照date来进行排序
     * @return 事件列表
     * @throws IOException
     */

    List<Event<Observation>> getObservationEventsByUserId(String userId, Date startDate, Date endDate);

    /**
     * 获取medication事件，按照start(事件开始事件)来进行排序
     * @return 事件列表
     * @throws IOException
     */

    List<Event<Medication>> getMedicationEventsByUserId(String userId, Date startDate, Date endDate);

    /**
     * 获取careplans事件，按照start来进行排序
     * @return 事件列表
     * @throws IOException
     */

    List<Event<CarePlan>> getCarePlanEventsByUserId(String userId, Date startDate, Date endDate);

    /**
     * 获取allergies事件，按照start(事件开始事件)来进行排序
     * @return 事件列表
     * @throws IOException
     */

    List<Event<Allergy>> getAllergyEventsByUserId(String userId, Date startDate, Date endDate);

    /**
     * 获取conditions事件，按照start(事件开始事件)来进行排序
     * @return 事件列表
     * @throws IOException
     */

    List<Event<Condition>> getConditionEventsByUserId(String userId, Date startDate, Date endDate);

    /**
     * 通过UserId来获取用户数据，由于对于每个User来说这个Event只会有唯一的一个，故获取其第一个元素即可
     * @param userId
     * @return 事件列表
     * @throws IOException
     */

    Event<UserBasic> getUserBasicByUserId(String userId);
}
