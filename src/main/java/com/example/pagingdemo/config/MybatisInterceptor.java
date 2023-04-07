package com.example.pagingdemo.config;

import com.example.pagingdemo.dto.Page;
import com.example.pagingdemo.dto.PageResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.builder.annotation.ProviderSqlSource;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.xmltags.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Intercepts({@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
@Component
public class MybatisInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        MappedStatement originalMappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameterObject = invocation.getArgs()[1];

        if(!originalMappedStatement.getSqlCommandType().equals("SELECT") && !(parameterObject instanceof Page)) {
            return invocation.proceed();
        }

        BoundSql boundSql = originalMappedStatement.getBoundSql(invocation.getArgs()[1]);
        String originalSql = boundSql.getSql();
//        originalMappedStatement.getSqlSource().get

//        SqlSource countSqlSource = modifySqlSource(originalMappedStatement, getTotalCountSql(originalSql));
//        invocation.getArgs()[0] = modifyMappedStatement(originalMappedStatement, countSqlSource, modifyResultMap(originalMappedStatement, Long.class));
//        List<Long> totalCount = (List<Long>) invocation.proceed();

        Page page = (Page) parameterObject;
        int offset = (page.getPageNumber() - 1) * page.getPageSize();
        int limit = page.getPageSize();

        SqlSource pagedSqlSource = modifySqlSource(originalMappedStatement, boundSql, getPagedQuery(originalSql, limit, offset));
        invocation.getArgs()[0] = modifyMappedStatement(originalMappedStatement, pagedSqlSource, originalMappedStatement.getResultMaps());
        List<Object> queryList = (List<Object>) invocation.proceed();

        SqlSource countSqlSource = modifySqlSource(originalMappedStatement, boundSql, getTotalCountSql(originalSql));
        invocation.getArgs()[0] = modifyMappedStatement(originalMappedStatement, countSqlSource, modifyResultMap(originalMappedStatement, Long.class));
        List<Long> totalCount = (List<Long>) invocation.proceed();


        return PageResponse.builder()
                .totalCount(totalCount.get(0))
                .list(queryList).build();
    }

    private String getTotalCountSql(String sql) {
        StringBuilder sb = new StringBuilder("SELECT COUNT(*) AS total_size FROM ( ");
        sb.append(sql).append(" ) T ");
        return sb.toString();
    }

    private String getPagedQuery(String sql, int limit, int offset) {
        StringBuilder sb = new StringBuilder(sql);
        sb.append(" LIMIT ").append(limit).append(" OFFSET ").append(offset);
        return sb.toString();
    }

    private SqlSource modifySqlSource(MappedStatement mappedStatement, BoundSql boundSql, String modifiedSql) {
        MetaObject metaObject = SystemMetaObject.forObject(mappedStatement.getSqlSource());
        SqlNode sqlNode = (SqlNode) metaObject.getValue("rootSqlNode");

        DynamicContext context = new DynamicContext(mappedStatement.getConfiguration(), boundSql.getParameterObject());
        sqlNode.apply(context);
        SqlSourceBuilder sqlSourceBuilder = new SqlSourceBuilder(mappedStatement.getConfiguration());
        Class<?> parameterType = boundSql.getParameterObject() == null ? Object.class : boundSql.getParameterObject().getClass();

        return sqlSourceBuilder.parse(modifiedSql, parameterType, context.getBindings());
//        mappedStatement.getSqlSource().
//        SqlNode newSqlNode = new StaticTextSqlNode(modifiedSql);
//        return new DynamicSqlSource(mappedStatement.getConfiguration(), newSqlNode);
    }

    private List<ResultMap> modifyResultMap(MappedStatement mappedStatement, Class<?> resultClass) {
        ResultMap resultMap = new ResultMap.Builder(mappedStatement.getConfiguration(), mappedStatement.getId(), resultClass, new ArrayList<>()).build();
        List<ResultMap> resultMaps = new ArrayList<>();
        resultMaps.add(resultMap);
        return resultMaps;
    }

    private MappedStatement modifyMappedStatement(MappedStatement mappedStatement, SqlSource newSqlSource, List<ResultMap> resultMaps) {
        return new MappedStatement.Builder(mappedStatement.getConfiguration(), mappedStatement.getId(), newSqlSource, mappedStatement.getSqlCommandType())
                .resultMaps(resultMaps)
                .statementType(mappedStatement.getStatementType())
                .cache(mappedStatement.getCache())
                .databaseId(mappedStatement.getDatabaseId())
                .fetchSize(mappedStatement.getFetchSize())
                .keyGenerator(mappedStatement.getKeyGenerator())
                .keyColumn(mappedStatement.getKeyColumns() != null ? String.join(",", mappedStatement.getKeyColumns()) : null)
                .keyProperty(mappedStatement.getKeyProperties() != null ? String.join(",", mappedStatement.getKeyProperties()) : null)
                .lang(mappedStatement.getLang())
                .parameterMap(mappedStatement.getParameterMap())
                .resource(mappedStatement.getResource())
                .resultOrdered(mappedStatement.isResultOrdered())
                .resultSets(mappedStatement.getResultSets() != null ? String.join(",", mappedStatement.getResultSets()) : null)
                .resultSetType(mappedStatement.getResultSetType())
                .flushCacheRequired(mappedStatement.isFlushCacheRequired())
                .timeout(mappedStatement.getTimeout())
                .useCache(mappedStatement.isUseCache()).build();
    }
}
