package org.owoto.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.owoto.entity.Article;
import org.owoto.entity.ArticleEs;
import org.owoto.mapper.ArticleDao;
import org.owoto.service.ArticleService;
import org.owoto.util.RedisUtil;
import org.owoto.util.ResultUtil;
import org.owoto.vo.PageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author zzf
 */
@RestController
@RequestMapping("article")
@Slf4j
@Api(tags = "文章管理")
public class ArticleController {
    static final String TAG_DESC = "tagDesc";
    static final String TITLE = "title";
    static final String CONTENT = "content";

    @Autowired
    ArticleDao articleMapper;
    @Autowired
    ArticleService articleService;
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;
    @Autowired
    RedisUtil redisUtil;

    @PostMapping("saveArticle")
    @ApiOperation("保存或修改文章")
    public Object saveArticle(@RequestBody Article article) {
        // 执行保存逻辑
        articleService.saveOrUpdate(article);
        /**
         * 刷新文章缓存,如果发布了就刷新
         * 刷新列表缓存的逻辑
         * 是否发布状态改变
         * 标题改变
         * 以及删除
         */
        String ak = "ARTICLE_DETAIL::" + article.getId();
        boolean b2 = redisUtil.hasKey(ak);
        Article article0 = articleService.getByCache(article.getId());
        Article article1 = articleService.getByDb(article.getId());
        boolean b0 = article0.getTitle().equals(article1.getTitle());
        boolean b1 = article0.getIsRelease().equals(article1.getIsRelease());
        if (!b0 || !b1 || !b2) {
            articleService.listByDb("");
            articleService.listByDb(article1.getTag());
        }
        return ResultUtil.success(article.getId());
    }

    @ApiOperation("文章分页列表")
    @GetMapping("non/page")
    public Object listArticles(PageVO pageVo, @RequestParam(defaultValue = "true") Boolean isOnlyRelease) {
        HashMap<String,String> map=new HashMap<>();
        map.put("updateTime","UPDATE_TIME");
        map.put("createTime","CREATE_TIME");
        QueryWrapper<Article> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(isOnlyRelease, "IS_RELEASE", 1).orderBy(null!=pageVo.getField(), "ascend".equals(pageVo.getOrder()),map.get(pageVo.getField())).orderByDesc("ORDER_NUM").orderByDesc("CREATE_TIME");
        IPage<Article> page = new Page<>(pageVo.getCurrent(), pageVo.getPageSize());
        IPage<Article> pageList = articleMapper.selectPage(page, queryWrapper);
        return ResultUtil.success(pageList);
    }

    @ApiOperation("排行榜")
    @GetMapping("non/hot")
    public Object listHotArticles() {
        return ResultUtil.success(redisUtil.getZSetRank("views", 0, -1));
    }

    @ApiOperation("文章总数")
    @GetMapping("countArticles")
    public Object countArticles() {
        return ResultUtil.success(articleMapper.selectCount(null));
    }

    @ApiOperation("文章分类")
    @GetMapping("/non/tags")
    public Object listTags() {
        return ResultUtil.success(articleMapper.getTags());
    }

    @ApiOperation("文章列表不分页")
    @GetMapping("non/list")
    public Object listArchives(@RequestParam(defaultValue = "") String code) {
        return ResultUtil.success(articleService.listByCache(code));
    }

    @ApiOperation("根据id查询文章详情-前台")
    @GetMapping("non/{id}")
    public Object getArticle(@PathVariable("id") String id) {
        if (StringUtils.isBlank(id)) {
            return ResultUtil.error("请传文章id");
        }
        Article article = articleService.getByCache(id);
        if (null == article || !article.getIsRelease()) {
            return ResultUtil.error("文章已下线");
        }
        article.setViewCount(redisUtil.incZSetValue("views", id, 1L).longValue());
        return ResultUtil.success(article);
    }

    @ApiOperation("根据id查询文章详情-后台")
    @GetMapping("{id}")
    public Object getArticleAdmin(@PathVariable("id") String id) {
        Article article = articleService.getByDb(id);
        return ResultUtil.success(article);
    }

    @ApiOperation("根据id删除文章")
    @DeleteMapping("non/{id}/{code}")
    public Object removeArticle(@PathVariable String id, @PathVariable String code) {
        articleService.listByDb("");
        articleService.listByDb(code);
        elasticsearchRestTemplate.delete(id, ArticleEs.class);
        return ResultUtil.success(articleMapper.deleteById(id));
    }

    @ApiOperation("es搜索")
    @GetMapping("non/search")
    public Object getList(String keyword) {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        queryBuilder.should(QueryBuilders.matchPhraseQuery(TITLE, keyword))
                .should(QueryBuilders.matchPhraseQuery(CONTENT, keyword))
                .should(QueryBuilders.matchPhraseQuery("tag_desc", keyword));
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder().withQuery(queryBuilder).withHighlightBuilder(new HighlightBuilder().field(TITLE).field(CONTENT).field("tag_desc")).build();
        List<ArticleEs> list = new ArrayList<>();
        SearchHits<ArticleEs> esSearchHits = elasticsearchRestTemplate.search(nativeSearchQuery, ArticleEs.class);
        esSearchHits.getSearchHits().forEach(searchHit -> {
            ArticleEs articleEs = searchHit.getContent();
            articleEs.setContent(StringUtils.join(searchHit.getHighlightField(CONTENT), " "));
            if (searchHit.getHighlightField(TAG_DESC).size() != 0) {
                articleEs.setTagDesc(StringUtils.join(searchHit.getHighlightField(TAG_DESC), " "));
            }
            if (searchHit.getHighlightField(TITLE).size() != 0) {
                articleEs.setTitle(StringUtils.join(searchHit.getHighlightField(TITLE), " "));
            }
            if (articleEs.getIsRelease()) {
                list.add(articleEs);
            }
        });
        return ResultUtil.success(list);
    }
}