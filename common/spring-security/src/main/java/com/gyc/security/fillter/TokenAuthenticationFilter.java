package com.gyc.security.fillter;

import com.alibaba.fastjson.JSON;
import com.gyc.common.jwt.JwtHelper;
import com.gyc.common.result.ResponseUtil;
import com.gyc.common.result.Result;
import com.gyc.common.result.ResultCodeEnum;
import com.gyc.security.custom.LoginUserInfoHelper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 认证解析 token过滤器，判断当前是否已经完成认证
 * </p>
 */
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private RedisTemplate redisTemplate;

    public TokenAuthenticationFilter(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        logger.info("uri:"+request.getRequestURI());
        //如果是登录接口，直接放行
        if("/admin/system/index/login".equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = getAuthentication(request);
        if(null != authentication) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } else {
            ResponseUtil.out(response, Result.build(null, ResultCodeEnum.PERMISSION));
        }
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        // 请求头是否有token
        String token = request.getHeader("token");
        logger.info("token:" + token);
        if (!StringUtils.isEmpty(token)) {
            String username = JwtHelper.getUsername(token);
            logger.info("useruame:" + username);
            if (!StringUtils.isEmpty(username)) {
                //当前用户信息放到ThreadLocal里面
                if (!StringUtils.isEmpty(username)) {
                    //通过ThreadLocal记录当前登录人信息
                    LoginUserInfoHelper.setUserId(JwtHelper.getUserId(token));
                    LoginUserInfoHelper.setUsername(username);
                    //通过username从redis获取权限数据
                    String authoritiesString = (String) redisTemplate.opsForValue().get(username);
                    //把redis获取字符串权限数据转换要求集合类型
                    List<Map> mapList = JSON.parseArray(authoritiesString, Map.class);
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    for (Map map : mapList) {
                        authorities.add(new SimpleGrantedAuthority((String) map.get("authority")));
                    }
                    return new UsernamePasswordAuthenticationToken(username, null, authorities);
                } else {
                    return new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
                }
            }

        }
        return null;
    }
}
