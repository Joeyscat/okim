package fun.oook.okim.client.controller;

import fun.oook.okim.client.client.CIMClient;
import fun.oook.okim.client.service.RouteRequest;
import fun.oook.okim.client.vo.req.GoogleProtocolVO;
import fun.oook.okim.client.vo.req.GroupReqVO;
import fun.oook.okim.client.vo.req.SendMsgReqVO;
import fun.oook.okim.client.vo.req.StringReqVO;
import fun.oook.okim.client.vo.res.SendMsgResVO;
import fun.oook.okim.common.enums.StatusEnum;
import fun.oook.okim.common.res.BaseResponse;
import fun.oook.okim.common.res.NULLBody;
import io.micrometer.core.instrument.Counter;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Function:
 *
 * @author crossoverJie
 * Date: 22/05/2018 14:46
 * @since JDK 1.8
 */
@Controller
@RequestMapping("/")
public class IndexController {

    /**
     * 统计 service
     */
    @Autowired
    private Counter clientPushCounter;

    @Autowired
    private CIMClient heartbeatClient;


    @Autowired
    private RouteRequest routeRequest;


    /**
     * 向服务端发消息 字符串
     *
     * @param stringReqVO
     * @return
     */
    @ApiOperation("客户端发送消息，字符串")
    @RequestMapping(value = "sendStringMsg", method = RequestMethod.POST)
    @ResponseBody
    public BaseResponse<NULLBody> sendStringMsg(@RequestBody StringReqVO stringReqVO) {
        BaseResponse<NULLBody> res = new BaseResponse();

        for (int i = 0; i < 100; i++) {
            heartbeatClient.sendStringMsg(stringReqVO.getMsg());
        }

        // 利用 actuator 来自增
        clientPushCounter.increment(1);

        SendMsgResVO sendMsgResVO = new SendMsgResVO();
        sendMsgResVO.setMsg("OK");
        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        return res;
    }

    /**
     * 向服务端发消息 Google ProtoBuf
     *
     * @param googleProtocolVO
     * @return
     */
    @ApiOperation("向服务端发消息 Google ProtoBuf")
    @RequestMapping(value = "sendProtoBufMsg", method = RequestMethod.POST)
    @ResponseBody
    public BaseResponse<NULLBody> sendProtoBufMsg(@RequestBody GoogleProtocolVO googleProtocolVO) {
        BaseResponse<NULLBody> res = new BaseResponse();

        for (int i = 0; i < 100; i++) {
            heartbeatClient.sendGoogleProtocolMsg(googleProtocolVO);
        }

        // 利用 actuator 来自增
        clientPushCounter.increment(1);

        SendMsgResVO sendMsgResVO = new SendMsgResVO();
        sendMsgResVO.setMsg("OK");
        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        return res;
    }


    /**
     * 群发消息
     *
     * @param sendMsgReqVO
     * @return
     */
    @ApiOperation("群发消息")
    @RequestMapping(value = "sendGroupMsg", method = RequestMethod.POST)
    @ResponseBody
    public BaseResponse sendGroupMsg(@RequestBody SendMsgReqVO sendMsgReqVO) throws Exception {
        BaseResponse<NULLBody> res = new BaseResponse();

        GroupReqVO groupReqVO = new GroupReqVO(sendMsgReqVO.getUserId(), sendMsgReqVO.getMsg());
        routeRequest.sendGroupMsg(groupReqVO);

        clientPushCounter.increment(1);

        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        return res;
    }
}
