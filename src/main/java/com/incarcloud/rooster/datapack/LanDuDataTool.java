package com.incarcloud.rooster.datapack;

import com.incarcloud.rooster.util.DataTool;
import io.netty.buffer.ByteBuf;
import org.springframework.stereotype.Component;

import static io.netty.buffer.Unpooled.buffer;

/**
 * Created by jackl on 2017/3/9.
 */
@Component
public class LanDuDataTool extends DataTool {


    public  static int getLanduCheckSum(byte[] bytes){
        String str = bytes2hex(bytes);
        //将字节数组进行异或操作求和
        ByteBuf bb = buffer(1024);

        String[] command = str.split(" ");
        int sum = 0;
        for(int i = 2; i < command.length;i++){
            sum += Integer.valueOf(command[i],16);
        }
        return sum;
    }

}
