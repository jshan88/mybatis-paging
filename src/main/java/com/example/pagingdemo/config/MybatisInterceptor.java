package com.example.pagingdemo.config;

import com.example.pagingdemo.dto.Page;
import com.example.pagingdemo.dto.PageResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;

@Slf4j
@Intercepts({@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
@Component
public class MybatisInterceptor implements Interceptor {

    // args[0] : MappedStatement, args[1] : Object (query parameter)
    private static final int MAPPED_STATEMENT_IDX = 0;
    private static final int PARAMETER_IDX = 1;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[MAPPED_STATEMENT_IDX];
        Object parameterObject = invocation.getArgs()[PARAMETER_IDX];

        if(!(parameterObject instanceof Page)) {
            return invocation.proceed();
        }

        BoundSql boundSql = mappedStatement.getBoundSql(parameterObject);
        String originalSql = boundSql.getSql();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();

        String countSql = toTotalCountSql(originalSql);
        BoundSql countBoundSql = replaceBoundSql(mappedStatement, boundSql, countSql, parameterMappings, parameterObject);
        SqlSource countSqlSource = new NewSqlSource(countBoundSql);
        invocation.getArgs()[MAPPED_STATEMENT_IDX] = replaceMappedStatement(mappedStatement, countSqlSource, replaceResultMap(mappedStatement, Long.class));
        List<Long> totalCounts = (List<Long>) invocation.proceed();

        String pagedSql = toPageSql(originalSql, parameterObject);
        BoundSql pageBoundSql = replaceBoundSql(mappedStatement, boundSql, pagedSql, parameterMappings, parameterObject);
        SqlSource pageSqlSource = new NewSqlSource(pageBoundSql);
        invocation.getArgs()[MAPPED_STATEMENT_IDX] = replaceMappedStatement(mappedStatement, pageSqlSource, mappedStatement.getResultMaps());
        List<Object> queryList = (List<Object>) invocation.proceed();

        return toPageResponse((Page) parameterObject, totalCounts.get(0), queryList);
    }

    private PageResponse toPageResponse(Page page, Long totalCount, List list) {
        int limit = page.getPageSize();
        int totalPage = (int) (totalCount / limit);
        if(totalCount % limit > 0) {
            totalPage += 1;
        }

        return PageResponse.builder()
            .totalCount(totalCount)
            .totalPage(totalPage)
            .currentPage(page.getPageNumber())
            .list(list)
            .build();
    }

    private String toTotalCountSql(String sql) {
        return new StringBuilder("SELECT COUNT(*) AS total_size FROM ( ")
            .append(sql)
            .append(" ) T")
            .toString();
    }

    private String toPageSql(String sql, Object parameterObject) {
        Page page = (Page) parameterObject;
        int offset = (page.getPageNumber() - 1) * page.getPageSize();
        int limit = page.getPageSize();

        return new StringBuilder(sql)
            .append(" LIMIT ")
            .append(limit)
            .append(" OFFSET ")
            .append(offset)
            .toString();
    }

    private BoundSql replaceBoundSql(MappedStatement ms, BoundSql boundSql, String newSql, List<ParameterMapping> parameterMappings, Object parameterObject) {
        BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), newSql, parameterMappings, parameterObject);
        for (ParameterMapping paramMapping : boundSql.getParameterMappings()) {
            String property = paramMapping.getProperty();
            if (boundSql.hasAdditionalParameter(property)) {
                newBoundSql.setAdditionalParameter(property, boundSql.getAdditionalParameter(property));
            }
        }
        return newBoundSql;
    }

    private List<ResultMap> replaceResultMap(MappedStatement mappedStatement, Class<?> resultClass) {
        ResultMap resultMap = new ResultMap.Builder(mappedStatement.getConfiguration(), mappedStatement.getId(), resultClass, new ArrayList<>()).build();
        List<ResultMap> resultMaps = new ArrayList<>();
        resultMaps.add(resultMap);
        return resultMaps;
    }

    private MappedStatement replaceMappedStatement(MappedStatement mappedStatement, SqlSource newSqlSource, List<ResultMap> resultMaps) {
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

    private static class NewSqlSource implements SqlSource{
        BoundSql boundSql;

        public NewSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }
        @Override
        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }
    }
}
