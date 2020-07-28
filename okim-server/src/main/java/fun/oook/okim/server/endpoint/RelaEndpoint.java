package fun.oook.okim.server.endpoint;

import fun.oook.okim.server.util.SessionSocketHolder;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author zhouyu
 */
@Endpoint(id = "relationship-endpoint")
public class RelaEndpoint {

    @ReadOperation
    public Map<Long, NioSocketChannel> endpoint(){
        return SessionSocketHolder.getRelationShip();
    }
}
