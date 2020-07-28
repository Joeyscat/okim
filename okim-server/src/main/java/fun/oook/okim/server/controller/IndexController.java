package fun.oook.okim.server.controller;

import fun.oook.okim.common.enums.StatusEnum;
import fun.oook.okim.common.res.BaseResponse;
import fun.oook.okim.server.api.ServerApi;
import fun.oook.okim.server.api.vo.req.SendMsgReqVO;
import fun.oook.okim.server.api.vo.res.SendMsgResVO;
import fun.oook.okim.server.server.CIMServer;
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
public class IndexController implements ServerApi {

    @Autowired
    private CIMServer cimServer;


    /**
     * 统计 service
     */
    @Autowired
    private Counter serverPushCounter;

    /**
     * @param sendMsgReqVO
     * @return
     */
    @Override
    @ApiOperation("Push msg to client")
    @RequestMapping(value = "sendMsg", method = RequestMethod.POST)
    @ResponseBody
    public BaseResponse<SendMsgResVO> sendMsg(@RequestBody SendMsgReqVO sendMsgReqVO) {
        BaseResponse<SendMsgResVO> res = new BaseResponse();
        cimServer.sendMsg(sendMsgReqVO);

        serverPushCounter.increment(1);

        SendMsgResVO sendMsgResVO = new SendMsgResVO();
        sendMsgResVO.setMsg("OK");
        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        res.setDataBody(sendMsgResVO);
        return res;
    }

}
