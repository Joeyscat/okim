package fun.oook.okim.client.thread;

import fun.oook.okim.client.service.impl.ClientHeartBeatHandlerImpl;
import fun.oook.okim.client.util.SpringBeanFactory;
import fun.oook.okim.common.kit.HeartBeatHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Function:
 *
 * @author crossoverJie
 * Date: 2019-01-20 21:35
 * @since JDK 1.8
 */
public class ReConnectJob implements Runnable {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReConnectJob.class);

    private ChannelHandlerContext context ;

    private HeartBeatHandler heartBeatHandler ;

    public ReConnectJob(ChannelHandlerContext context) {
        this.context = context;
        this.heartBeatHandler = SpringBeanFactory.getBean(ClientHeartBeatHandlerImpl.class) ;
    }

    @Override
    public void run() {
        try {
            heartBeatHandler.process(context);
        } catch (Exception e) {
            LOGGER.error("Exception",e);
        }
    }
}
