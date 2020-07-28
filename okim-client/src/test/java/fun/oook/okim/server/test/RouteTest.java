package fun.oook.okim.server.test;

import fun.oook.okim.client.OKIMClientApplication;
import fun.oook.okim.client.service.RouteRequest;
import fun.oook.okim.client.vo.req.LoginReqVO;
import fun.oook.okim.client.vo.res.CIMServerResVO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Function:
 *
 * @author crossoverJie
 *         Date: 2018/12/23 22:39
 * @since JDK 1.8
 */
@SpringBootTest(classes = OKIMClientApplication.class)
@RunWith(SpringRunner.class)
public class RouteTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(RouteTest.class);

    @Value("${okim.user.id}")
    private long userId;

    @Value("${okim.user.userName}")
    private String userName;

    @Autowired
    private RouteRequest routeRequest ;

    @Test
    public void test() throws Exception {
        LoginReqVO vo = new LoginReqVO(userId,userName) ;
        CIMServerResVO.ServerInfo cimServer = routeRequest.getCIMServer(vo);
        LOGGER.info("cimServer=[{}]",cimServer.toString());
    }
}
