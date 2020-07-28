package fun.oook.okim.route.controller;

import fun.oook.okim.common.enums.StatusEnum;
import fun.oook.okim.common.exception.CIMException;
import fun.oook.okim.common.pojo.CIMUserInfo;
import fun.oook.okim.common.pojo.RouteInfo;
import fun.oook.okim.common.res.BaseResponse;
import fun.oook.okim.common.res.NULLBody;
import fun.oook.okim.common.route.algorithm.RouteHandle;
import fun.oook.okim.common.util.RouteInfoParseUtil;
import fun.oook.okim.route.api.RouteApi;
import fun.oook.okim.route.api.vo.req.ChatReqVO;
import fun.oook.okim.route.api.vo.req.LoginReqVO;
import fun.oook.okim.route.api.vo.req.P2PReqVO;
import fun.oook.okim.route.api.vo.req.RegisterInfoReqVO;
import fun.oook.okim.route.api.vo.res.CIMServerResVO;
import fun.oook.okim.route.api.vo.res.RegisterInfoResVO;
import fun.oook.okim.route.cache.ServerCache;
import fun.oook.okim.route.service.AccountService;
import fun.oook.okim.route.service.CommonBizService;
import fun.oook.okim.route.service.UserInfoCacheService;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.Set;

/**
 * Function:
 *
 * @author crossoverJie
 *         Date: 22/05/2018 14:46
 * @since JDK 1.8
 */
@Controller
@RequestMapping("/")
public class RouteController implements RouteApi {
    private final static Logger LOGGER = LoggerFactory.getLogger(RouteController.class);

    @Autowired
    private ServerCache serverCache;

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserInfoCacheService userInfoCacheService ;

    @Autowired
    private CommonBizService commonBizService ;

    @Autowired
    private RouteHandle routeHandle ;

    @ApiOperation("群聊 API")
    @RequestMapping(value = "groupRoute", method = RequestMethod.POST)
    @ResponseBody()
    @Override
    public BaseResponse<NULLBody> groupRoute(@RequestBody ChatReqVO groupReqVO) throws Exception {
        BaseResponse<NULLBody> res = new BaseResponse<>();

        LOGGER.info("msg=[{}]", groupReqVO.toString());

        //获取所有的推送列表
        Map<Long, CIMServerResVO> serverResVOMap = accountService.loadRouteRelated();
        for (Map.Entry<Long, CIMServerResVO> cimServerResVOEntry : serverResVOMap.entrySet()) {
            Long userId = cimServerResVOEntry.getKey();
            CIMServerResVO cimServerResVO = cimServerResVOEntry.getValue();
            if (userId.equals(groupReqVO.getUserId())){
                //过滤掉自己
                CIMUserInfo cimUserInfo = userInfoCacheService.loadUserInfoByUserId(groupReqVO.getUserId());
                LOGGER.warn("过滤掉了发送者 userId={}",cimUserInfo.toString());
                continue;
            }

            //推送消息
            ChatReqVO chatVO = new ChatReqVO(userId,groupReqVO.getMsg()) ;
            accountService.pushMsg(cimServerResVO ,groupReqVO.getUserId(),chatVO);

        }

        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        return res;
    }


    /**
     * 私聊路由
     *
     * @param p2pRequest
     * @return
     */
    @ApiOperation("私聊 API")
    @RequestMapping(value = "p2pRoute", method = RequestMethod.POST)
    @ResponseBody()
    @Override
    public BaseResponse<NULLBody> p2pRoute(@RequestBody P2PReqVO p2pRequest) throws Exception {
        BaseResponse<NULLBody> res = new BaseResponse<>();

        try {
            //获取接收消息用户的路由信息
            CIMServerResVO cimServerResVO = accountService.loadRouteRelatedByUserId(p2pRequest.getReceiveUserId());

            //p2pRequest.getReceiveUserId()==>消息接收者的 userID
            ChatReqVO chatVO = new ChatReqVO(p2pRequest.getReceiveUserId(),p2pRequest.getMsg()) ;
            accountService.pushMsg(cimServerResVO ,p2pRequest.getUserId(),chatVO);

            res.setCode(StatusEnum.SUCCESS.getCode());
            res.setMessage(StatusEnum.SUCCESS.getMessage());

        }catch (CIMException e){
            res.setCode(e.getErrorCode());
            res.setMessage(e.getErrorMessage());
        }
        return res;
    }


    @ApiOperation("客户端下线")
    @RequestMapping(value = "offLine", method = RequestMethod.POST)
    @ResponseBody()
    @Override
    public BaseResponse<NULLBody> offLine(@RequestBody ChatReqVO groupReqVO) throws Exception {
        BaseResponse<NULLBody> res = new BaseResponse<>();

        CIMUserInfo cimUserInfo = userInfoCacheService.loadUserInfoByUserId(groupReqVO.getUserId());

        LOGGER.info("user [{}] offline!", cimUserInfo.toString());
        accountService.offLine(groupReqVO.getUserId());

        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        return res;
    }

    /**
     * 获取一台 CIM server
     *
     * @return
     */
    @ApiOperation("登录并获取服务器")
    @RequestMapping(value = "login", method = RequestMethod.POST)
    @ResponseBody()
    @Override
    public BaseResponse<CIMServerResVO> login(@RequestBody LoginReqVO loginReqVO) throws Exception {
        BaseResponse<CIMServerResVO> res = new BaseResponse<>();

        // check server available
        String server = routeHandle.routeServer(serverCache.getServerList(),String.valueOf(loginReqVO.getUserId()));
        LOGGER.info("userName=[{}] route server info=[{}]", loginReqVO.getUserName(), server);

        RouteInfo routeInfo = RouteInfoParseUtil.parse(server);
        commonBizService.checkServerAvailable(routeInfo);

        //登录校验
        StatusEnum status = accountService.login(loginReqVO);
        if (status == StatusEnum.SUCCESS) {

            //保存路由信息
            accountService.saveRouteInfo(loginReqVO,server);

            CIMServerResVO vo = new CIMServerResVO(routeInfo);
            res.setDataBody(vo);

        }
        res.setCode(status.getCode());
        res.setMessage(status.getMessage());

        return res;
    }

    /**
     * 注册账号
     *
     * @return
     */
    @ApiOperation("注册账号")
    @RequestMapping(value = "registerAccount", method = RequestMethod.POST)
    @ResponseBody()
    @Override
    public BaseResponse<RegisterInfoResVO> registerAccount(@RequestBody RegisterInfoReqVO registerInfoReqVO) throws Exception {
        BaseResponse<RegisterInfoResVO> res = new BaseResponse<>();

        long userId = System.currentTimeMillis();
        RegisterInfoResVO info = new RegisterInfoResVO(userId, registerInfoReqVO.getUserName());
        info = accountService.register(info);

        res.setDataBody(info);
        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        return res;
    }

    /**
     * 获取所有在线用户
     *
     * @return
     */
    @ApiOperation("获取所有在线用户")
    @RequestMapping(value = "onlineUser", method = RequestMethod.POST)
    @ResponseBody()
    @Override
    public BaseResponse<Set<CIMUserInfo>> onlineUser() throws Exception {
        BaseResponse<Set<CIMUserInfo>> res = new BaseResponse<>();

        Set<CIMUserInfo> cimUserInfos = userInfoCacheService.onlineUser();
        res.setDataBody(cimUserInfos) ;
        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        return res;
    }





}
