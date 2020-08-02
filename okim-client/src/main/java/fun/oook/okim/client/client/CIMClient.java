package fun.oook.okim.client.client;

import fun.oook.okim.client.config.AppConfiguration;
import fun.oook.okim.client.init.CIMClientHandleInitializer;
import fun.oook.okim.client.service.EchoService;
import fun.oook.okim.client.service.MsgHandle;
import fun.oook.okim.client.service.ReConnectManager;
import fun.oook.okim.client.service.RouteRequest;
import fun.oook.okim.client.service.impl.ClientInfo;
import fun.oook.okim.client.thread.ContextHolder;
import fun.oook.okim.client.vo.req.GoogleProtocolVO;
import fun.oook.okim.client.vo.req.LoginReqVO;
import fun.oook.okim.client.vo.res.CIMServerResVO;
import fun.oook.okim.common.constant.Constants;
import fun.oook.okim.common.protocol.CIMRequestProto;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Function:
 *
 * @author crossoverJie
 * Date: 22/05/2018 14:19
 * @since JDK 1.8
 */
@Component
public class CIMClient {

    private final static Logger LOGGER = LoggerFactory.getLogger(CIMClient.class);

    private EventLoopGroup group = new NioEventLoopGroup(0, new DefaultThreadFactory("okim-work"));

    @Value("${okim.user.id}")
    private long userId;

    @Value("${okim.user.userName}")
    private String userName;

    private SocketChannel channel;

    @Autowired
    private EchoService echoService;

    @Autowired
    private RouteRequest routeRequest;

    @Autowired
    private AppConfiguration configuration;

    @Autowired
    private MsgHandle msgHandle;

    @Autowired
    private ClientInfo clientInfo;

    @Autowired
    private ReConnectManager reConnectManager;

    /**
     * 重试次数
     */
    private int errorCount;

    @PostConstruct
    public void start() throws Exception {

        //登录 + 获取可以使用的服务器 ip+port
        CIMServerResVO.ServerInfo cimServer = userLogin();

        //启动客户端
        startClient(cimServer);

        //向服务端注册
        loginCIMServer();
    }

    /**
     * 启动客户端
     *
     * @param cimServer
     * @throws Exception
     */
    private void startClient(CIMServerResVO.ServerInfo cimServer) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new CIMClientHandleInitializer());

        ChannelFuture future = bootstrap.connect(cimServer.getIp(), cimServer.getCimServerPort()).sync();

        if (future != null && future.isSuccess()) {
            echoService.echo("Start okim client success!");
            LOGGER.info("启动 okim client 成功");
            channel = (SocketChannel) future.channel();
        } else {
            errorCount++;

            if (errorCount >= configuration.getErrorCount()) {
                LOGGER.error("连接失败次数达到上限[{}]次", errorCount);
                msgHandle.shutdown();
            }
            LOGGER.error("Connect fail!");
        }
    }

    /**
     * 登录+路由服务器
     *
     * @return 路由服务器信息
     * @throws Exception
     */
    private CIMServerResVO.ServerInfo userLogin() throws Exception {
        LoginReqVO loginReqVO = new LoginReqVO(userId, userName);
        CIMServerResVO.ServerInfo cimServer = routeRequest.getCIMServer(loginReqVO);

        if (cimServer != null) {
            //保存系统信息
            clientInfo.saveServiceInfo(cimServer.getIp() + ":" + cimServer.getCimServerPort())
                    .saveUserInfo(userId, userName);

            LOGGER.info("cimServer=[{}]", cimServer.toString());
        } else {
            errorCount++;

            if (errorCount >= configuration.getErrorCount()) {
                echoService.echo("The maximum number of reconnections has been reached[{}]times, close okim client!", errorCount);
                msgHandle.shutdown();
            }
            LOGGER.error("login fail");
        }

        return cimServer;
    }

    /**
     * 向服务器注册
     */
    private void loginCIMServer() {
        CIMRequestProto.CIMReqProtocol login = CIMRequestProto.CIMReqProtocol.newBuilder()
                .setRequestId(userId)
                .setReqMsg(userName)
                .setType(Constants.CommandType.LOGIN)
                .build();
        ChannelFuture future = channel.writeAndFlush(login);
        future.addListener((ChannelFutureListener) channelFuture ->
                echoService.echo("Registry okim server success!")
        );
    }

    /**
     * 发送消息字符串
     *
     * @param msg
     */
    public void sendStringMsg(String msg) {
        ByteBuf message = Unpooled.buffer(msg.getBytes().length);
        message.writeBytes(msg.getBytes());
        ChannelFuture future = channel.writeAndFlush(message);
        future.addListener((ChannelFutureListener) channelFuture ->
                LOGGER.info("客户端手动发消息成功={}", msg));

    }

    /**
     * 发送 Google Protocol 编解码字符串
     *
     * @param googleProtocolVO
     */
    public void sendGoogleProtocolMsg(GoogleProtocolVO googleProtocolVO) {

        CIMRequestProto.CIMReqProtocol protocol = CIMRequestProto.CIMReqProtocol.newBuilder()
                .setRequestId(googleProtocolVO.getRequestId())
                .setReqMsg(googleProtocolVO.getMsg())
                .setType(Constants.CommandType.MSG)
                .build();


        ChannelFuture future = channel.writeAndFlush(protocol);
        future.addListener((ChannelFutureListener) channelFuture ->
                LOGGER.info("客户端手动发送 Google Protocol 成功={}", googleProtocolVO.toString()));

    }


    /**
     * 1. clear route information.
     * 2. reconnect.
     * 3. shutdown reconnect job.
     * 4. reset reconnect state.
     *
     * @throws Exception
     */
    public void reconnect() throws Exception {
        if (channel != null && channel.isActive()) {
            return;
        }
        //首先清除路由信息，下线
        routeRequest.offLine();

        echoService.echo("okim server shutdown, reconnecting....");
        start();
        echoService.echo("Great! reConnect success!!!");
        reConnectManager.reConnectSuccess();
        ContextHolder.clear();
    }

    /**
     * 关闭
     *
     * @throws InterruptedException
     */
    public void close() throws InterruptedException {
        if (channel != null) {
            channel.close();
        }
    }
}
