package io.mcache.sdk.proto;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Hand-written proto3 message classes for mcache.proto.
 * Each class implements manual wire-format encode/decode to avoid protoc.
 *
 * Proto3 wire types: 0=varint, 2=length-delimited
 * Field tags: (field_number << 3) | wire_type
 */
public final class Messages {

    private Messages() {}

    // ---- InsertRequest ------------------------------------------------

    public static final class InsertRequest {
        public String prefix = "";
        public byte[] data = new byte[0];
        public long ttlSeconds;

        public byte[] toByteArray() throws IOException {
            int size = 0;
            if (!prefix.isEmpty()) size += CodedOutputStream.computeStringSize(1, prefix);
            if (data.length > 0) size += CodedOutputStream.computeByteArraySize(2, data);
            if (ttlSeconds != 0) size += CodedOutputStream.computeInt64Size(3, ttlSeconds);
            byte[] buf = new byte[size];
            CodedOutputStream out = CodedOutputStream.newInstance(buf);
            if (!prefix.isEmpty()) out.writeString(1, prefix);
            if (data.length > 0) out.writeByteArray(2, data);
            if (ttlSeconds != 0) out.writeInt64(3, ttlSeconds);
            out.flush();
            return buf;
        }
    }

    // ---- InsertResponse -----------------------------------------------

    public static final class InsertResponse {
        public boolean success;

        public static InsertResponse parseFrom(byte[] bytes) throws InvalidProtocolBufferException {
            InsertResponse r = new InsertResponse();
            try {
                CodedInputStream in = CodedInputStream.newInstance(bytes);
                int tag;
                while ((tag = in.readTag()) != 0) {
                    if (tag == 8) r.success = in.readBool(); else in.skipField(tag);
                }
            } catch (IOException e) { throw new InvalidProtocolBufferException(e.getMessage()); }
            return r;
        }
    }

    // ---- GetRequest ---------------------------------------------------

    public static final class GetRequest {
        public String prefix = "";

        public byte[] toByteArray() throws IOException {
            if (prefix.isEmpty()) return new byte[0];
            int size = CodedOutputStream.computeStringSize(1, prefix);
            byte[] buf = new byte[size];
            CodedOutputStream out = CodedOutputStream.newInstance(buf);
            out.writeString(1, prefix);
            out.flush();
            return buf;
        }
    }

    // ---- GetResponse --------------------------------------------------

    public static final class GetResponse {
        public String prefix = "";
        public byte[] data = new byte[0];
        public long expireTime;
        public long createdAt;
        public long updatedAt;

        public static GetResponse parseFrom(byte[] bytes) throws InvalidProtocolBufferException {
            GetResponse r = new GetResponse();
            try {
                CodedInputStream in = CodedInputStream.newInstance(bytes);
                int tag;
                while ((tag = in.readTag()) != 0) {
                    switch (tag) {
                        case 10: r.prefix = in.readStringRequireUtf8(); break;
                        case 18: r.data = in.readByteArray(); break;
                        case 24: r.expireTime = in.readInt64(); break;
                        case 32: r.createdAt = in.readInt64(); break;
                        case 40: r.updatedAt = in.readInt64(); break;
                        default: in.skipField(tag);
                    }
                }
            } catch (IOException e) { throw new InvalidProtocolBufferException(e.getMessage()); }
            return r;
        }
    }

    // ---- UpdateRequest ------------------------------------------------

    public static final class UpdateRequest {
        public String prefix = "";
        public byte[] data = new byte[0];
        public long ttlSeconds;

        public byte[] toByteArray() throws IOException {
            int size = 0;
            if (!prefix.isEmpty()) size += CodedOutputStream.computeStringSize(1, prefix);
            if (data.length > 0) size += CodedOutputStream.computeByteArraySize(2, data);
            if (ttlSeconds != 0) size += CodedOutputStream.computeInt64Size(3, ttlSeconds);
            byte[] buf = new byte[size];
            CodedOutputStream out = CodedOutputStream.newInstance(buf);
            if (!prefix.isEmpty()) out.writeString(1, prefix);
            if (data.length > 0) out.writeByteArray(2, data);
            if (ttlSeconds != 0) out.writeInt64(3, ttlSeconds);
            out.flush();
            return buf;
        }
    }

    // ---- UpdateResponse -----------------------------------------------

    public static final class UpdateResponse {
        public boolean success;

        public static UpdateResponse parseFrom(byte[] bytes) throws InvalidProtocolBufferException {
            UpdateResponse r = new UpdateResponse();
            try {
                CodedInputStream in = CodedInputStream.newInstance(bytes);
                int tag;
                while ((tag = in.readTag()) != 0) {
                    if (tag == 8) r.success = in.readBool(); else in.skipField(tag);
                }
            } catch (IOException e) { throw new InvalidProtocolBufferException(e.getMessage()); }
            return r;
        }
    }

    // ---- DeleteRequest ------------------------------------------------

    public static final class DeleteRequest {
        public String prefix = "";

        public byte[] toByteArray() throws IOException {
            if (prefix.isEmpty()) return new byte[0];
            int size = CodedOutputStream.computeStringSize(1, prefix);
            byte[] buf = new byte[size];
            CodedOutputStream out = CodedOutputStream.newInstance(buf);
            out.writeString(1, prefix);
            out.flush();
            return buf;
        }
    }

    // ---- DeleteResponse -----------------------------------------------

    public static final class DeleteResponse {
        public boolean success;

        public static DeleteResponse parseFrom(byte[] bytes) throws InvalidProtocolBufferException {
            DeleteResponse r = new DeleteResponse();
            try {
                CodedInputStream in = CodedInputStream.newInstance(bytes);
                int tag;
                while ((tag = in.readTag()) != 0) {
                    if (tag == 8) r.success = in.readBool(); else in.skipField(tag);
                }
            } catch (IOException e) { throw new InvalidProtocolBufferException(e.getMessage()); }
            return r;
        }
    }

    // ---- ListByPrefixRequest ------------------------------------------

    public static final class ListByPrefixRequest {
        public String prefix = "";

        public byte[] toByteArray() throws IOException {
            if (prefix.isEmpty()) return new byte[0];
            int size = CodedOutputStream.computeStringSize(1, prefix);
            byte[] buf = new byte[size];
            CodedOutputStream out = CodedOutputStream.newInstance(buf);
            out.writeString(1, prefix);
            out.flush();
            return buf;
        }
    }

    // ---- ListByPrefixResponse -----------------------------------------

    public static final class ListByPrefixResponse {
        public List<GetResponse> items = new ArrayList<>();

        public static ListByPrefixResponse parseFrom(byte[] bytes) throws InvalidProtocolBufferException {
            ListByPrefixResponse r = new ListByPrefixResponse();
            try {
                CodedInputStream in = CodedInputStream.newInstance(bytes);
                int tag;
                while ((tag = in.readTag()) != 0) {
                    if (tag == 10) {
                        byte[] nested = in.readByteArray();
                        r.items.add(GetResponse.parseFrom(nested));
                    } else {
                        in.skipField(tag);
                    }
                }
            } catch (IOException e) { throw new InvalidProtocolBufferException(e.getMessage()); }
            return r;
        }
    }
}
