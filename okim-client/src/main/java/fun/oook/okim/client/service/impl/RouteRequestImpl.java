package fun.oook.okim.client.service.impl;

import com.alibaba.fastjson.JSON;
import fun.oook.okim.client.config.AppConfiguration;
import fun.oook.okim.client.service.EchoService;
import fun.oook.okim.client.service.RouteRequest;
import fun.oook.okim.client.thread.ContextHolder;
import fun.oook.okim.client.vo.req.GroupReqVO;
import fun.oook.okim.client.vo.req.LoginReqVO;
import fun.oook.okim.client.vo.req.P2PReqVO;
import fun.oook.okim.client.vo.res.CIMServerResVO;
import fun.oook.okim.client.vo.res.OnlineUsersResVO;
import fun.oook.okim.common.core.proxy.ProxyManager;
import fun.oook.okim.common.enums.StatusEnum;
import fun.oook.okim.common.exception.CIMException;
import fun.oook.okim.common.res.BaseResponse;
import fun.oook.okim.route.api.RouteApi;
import fun.oook.okim.route.api.vo.req.ChatReqVO;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Function:
 *
 * @author crossoverJie
 * Date: 2018/12/22 22:27
 * @since JDK 1.8
 */
@Service
public class RouteRequestImpl implements RouteRequest {

    private final static Logger LOGGER = LoggerFactory.getLogger(RouteRequestImpl.class);

    @Autowired
    private OkHttpClient okHttpClient;

    @Value("${okim.route.url}")
    private String routeUrl;

    @Autowired
    private EchoService echoService;

    @Autowired
    private AppConfiguration appConfiguration;

    @Override
    public void sendGroupMsg(GroupReqVO groupReqVO) throws Exception {
        RouteApi routeApi = new ProxyManager<>(RouteApi.class, routeUrl, okHttpClient).getInstance();
        ChatReqVO chatReqVO = new ChatReqVO(groupReqVO.getUserId(), groupReqVO.getMsg());
        Response response = (Response) routeApi.groupRoute(chatReqVO);
        response.body().close();
    }

    @Override
    public void sendP2PMsg(P2PReqVO reqVO) throws Exception {
        RouteApi routeApi = new ProxyManager<>(RouteApi.class, routeUrl, okHttpClient).getInstance();
        fun.oook.okim.route.api.vo.req.P2PReqVO vo = new fun.oook.okim.route.api.vo.req.P2PReqVO();
        vo.setMsg(reqVO.getMsg());
        vo.setReceiveUserId(reqVO.getReceiveUserId());
        vo.setUserId(reqVO.getUserId());

        Response response = (Response) routeApi.p2pRoute(vo);
        String json = response.body().string();
        BaseResponse baseResponse = JSON.parseObject(json, BaseResponse.class);

        // account offline.
        if (baseResponse.getCode().equals(StatusEnum.OFF_LINE.getCode())) {
            LOGGER.error(reqVO.getReceiveUserId() + ":" + StatusEnum.OFF_LINE.getMessage());
        }
        response.body().close();
    }

    @Override
    public CIMServerResVO.ServerInfo getCIMServer(LoginReqVO loginReqVO) throws Exception {

        RouteApi routeApi = new ProxyManager<>(RouteApi.class, routeUrl, okHttpClient).getInstance();
        fun.oook.okim.route.api.vo.req.LoginReqVO vo = new fun.oook.okim.route.api.vo.req.LoginReqVO();
        vo.setUserId(loginReqVO.getUserId());
        vo.setUserName(loginReqVO.getUserName());

        Response response = (Response) routeApi.login(vo);
        String json = response.body().string();
        CIMServerResVO cimServerResVO = JSON.parseObject(json, CIMServerResVO.class);

        //重复失败
        if (!cimServerResVO.getCode().equals(StatusEnum.SUCCESS.getCode())) {
            echoService.echo(cimServerResVO.getMessage());

            // when client in reConnect state, could not exit.
            if (ContextHolder.getReconnect() != null && ContextHolder.getReconnect()) {
                echoService.echo("###{}###", StatusEnum.RECONNECT_FAIL.getMessage());
                throw new CIMException(StatusEnum.RECONNECT_FAIL);
            }

            System.exit(-1);
        }

        response.body().close();

        return cimServerResVO.getDataBody();
    }

    @Override
    public List<OnlineUsersResVO.DataBodyBean> onlineUsers() throws Exception {
        RouteApi routeApi = new ProxyManager<>(RouteApi.class, routeUrl, okHttpClient).getInstance();

        Response response = (Response) routeApi.onlineUser();
        String json = response.body().string();
        OnlineUsersResVO onlineUsersResVO = JSON.parseObject(json, OnlineUsersResVO.class);

        response.body().close();
        return onlineUsersResVO.getDataBody();
    }

    @Override
    public void offLine() {
        RouteApi routeApi = new ProxyManager<>(RouteApi.class, routeUrl, okHttpClient).getInstance();
        ChatReqVO vo = new ChatReqVO(appConfiguration.getUserId(), "offLine");
        Response response = null;
        try {
            response = (Response) routeApi.offLine(vo);
        } catch (Exception e) {
            LOGGER.error("exception", e);
        } finally {
            if (response != null) {
                response.body().close();
            }
        }
    }
}
