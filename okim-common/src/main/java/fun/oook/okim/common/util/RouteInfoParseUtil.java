package fun.oook.okim.common.util;

import fun.oook.okim.common.exception.CIMException;
import fun.oook.okim.common.pojo.RouteInfo;
import fun.oook.okim.common.enums.StatusEnum;

/**
 * Function:
 *
 * @author crossoverJie
 * Date: 2020-04-12 20:42
 * @since JDK 1.8
 */
public class RouteInfoParseUtil {

    public static RouteInfo parse(String info){
        try {
            String[] serverInfo = info.split(":");
            RouteInfo routeInfo =  new RouteInfo(serverInfo[0], Integer.parseInt(serverInfo[1]),Integer.parseInt(serverInfo[2])) ;
            return routeInfo ;
        }catch (Exception e){
            throw new CIMException(StatusEnum.VALIDATION_FAIL) ;
        }
    }
}
