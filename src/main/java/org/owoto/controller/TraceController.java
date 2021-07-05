package org.owoto.controller;

import com.alibaba.druid.util.DruidWebUtils;
import com.alibaba.fastjson.JSON;
import org.owoto.component.Send;
import org.owoto.entity.Trace;
import org.owoto.service.TraceService;
import org.owoto.util.HttpUtil;
import org.owoto.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * TRACE(Trace)表控制层
 *
 * @author makejava
 * @since 2021-06-25 16:22:10
 */
@RestController
@RequestMapping("trace")
public class TraceController {
    /**
     * 服务对象
     */
    @Resource
    private TraceService traceService;
    @Autowired
    private Send send;

    @PostMapping("non/save")
    public Object selectOne(@RequestBody Trace trace, HttpServletRequest request) {
        return ResultUtil.success(this.traceService.save(trace));
    }
    @GetMapping("non/ip")
    public Object selectOne(HttpServletRequest request) {
        return ResultUtil.success(DruidWebUtils.getRemoteAddr(request));
    }
    @GetMapping("non/ips")
    public Object selectOne() {
        return ResultUtil.success(HttpUtil.getIp());
    }
    @GetMapping("non/send")
    public Object send(String request) {
        send.post();
        return ResultUtil.success(null);
    }
    @GetMapping("server/info")
    public Object sys(){
        RestTemplate restTemplate = new RestTemplate();
        String res=restTemplate.getForObject("http://47.100.47.169/json/stats.json",String.class);
        return ResultUtil.success(JSON.parse(res));
    }
}