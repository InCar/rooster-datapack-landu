package com.incarcloud.rooster.datapack;/**
 * Created by fanbeibei on 2017/7/21.
 */

import com.incarcloud.rooster.gather.cmd.CommandType;

/**
 * @author Fan Beibei
 * @Description: 描述
 * @date 2017/7/21 14:38
 */
public class LanduCommandFacotry implements CommandFacotry {

    static {
        CommandFacotryManager.registerCommandFacotry("china-landu-3.08",LanduCommandFacotry.class);
    }


    /**
     * 创建二进制命令,若不支持此命令则返回null
     *
     * @param type     命令类型
     * @return
     */
    public byte[] createCommand(CommandType type){//TODO 待实现

        byte[] cmd = "111111111".getBytes();

        return cmd;


    }
}
