package com.incarcloud.rooster.datapack;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

public class DataParserLANDU implements IDataParser {

    @Override
    public List<DataPack> extract(ByteBuf buffer){
        List<DataPack> listPacks = new ArrayList<>();

        return listPacks;
    }
}
