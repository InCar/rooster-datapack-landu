package com.incarcloud.rooster.datapack;/**
 * Created by fanbeibei on 2017/7/21.
 */

import com.incarcloud.rooster.gather.cmd.CommandType;
import io.netty.buffer.ByteBuf;

/**
 * @author Fan Beibei
 * @Description: 描述
 * @date 2017/7/21 14:38
 */
public class LanduCommandFacotry implements CommandFactory {

    static {
        CommandFacotryManager.registerCommandFacotry(DataParserLandu.PROTOCOL_PREFIX + "2.05", LanduCommandFacotry.class);
        CommandFacotryManager.registerCommandFacotry(DataParserLandu.PROTOCOL_PREFIX + "3.08", LanduCommandFacotry.class);
    }


    /**
     * 创建二进制命令,若不支持此命令则返回null
     *
     * @param type 命令类型
     * @return
     */
    public ByteBuf createCommand(CommandType type, Object... args) {
        // 目前LANDU(2.05/3.08)不支持CommandType类型
        return null;
    }
}
