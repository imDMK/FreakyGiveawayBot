package me.dmk.app.database.codec;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.Date;

/**
 * Created by DMK on 30.03.2023
 */

public class DateCodec implements Codec<Date> {

    @Override
    public Date decode(BsonReader reader, DecoderContext decoderContext) {
        return new Date(reader.readInt64());
    }

    @Override
    public void encode(BsonWriter writer, Date date, EncoderContext encoderContext) {
        writer.writeInt64(date.getTime());
    }

    @Override
    public Class<Date> getEncoderClass() {
        return Date.class;
    }
}
