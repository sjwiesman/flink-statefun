//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// Code generated by protoc-gen-go. DO NOT EDIT.
// versions:
// 	protoc-gen-go v1.23.0
// 	protoc        v3.12.3
// source: statefun-flink/statefun-flink-io/src/main/protobuf/kinesis-egress.proto

package messages

import (
	proto "github.com/golang/protobuf/proto"
	protoreflect "google.golang.org/protobuf/reflect/protoreflect"
	protoimpl "google.golang.org/protobuf/runtime/protoimpl"
	reflect "reflect"
	sync "sync"
)

const (
	// Verify that this generated code is sufficiently up-to-date.
	_ = protoimpl.EnforceVersion(20 - protoimpl.MinVersion)
	// Verify that runtime/protoimpl is sufficiently up-to-date.
	_ = protoimpl.EnforceVersion(protoimpl.MaxVersion - 20)
)

// This is a compile-time assertion that a sufficiently up-to-date version
// of the legacy proto package is being used.
const _ = proto.ProtoPackageIsVersion4

type KinesisEgressRecord struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	PartitionKey    string `protobuf:"bytes,1,opt,name=partition_key,json=partitionKey,proto3" json:"partition_key,omitempty"`
	ValueBytes      []byte `protobuf:"bytes,2,opt,name=value_bytes,json=valueBytes,proto3" json:"value_bytes,omitempty"`
	Stream          string `protobuf:"bytes,3,opt,name=stream,proto3" json:"stream,omitempty"`
	ExplicitHashKey string `protobuf:"bytes,4,opt,name=explicit_hash_key,json=explicitHashKey,proto3" json:"explicit_hash_key,omitempty"`
}

func (x *KinesisEgressRecord) Reset() {
	*x = KinesisEgressRecord{}
	if protoimpl.UnsafeEnabled {
		mi := &file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_msgTypes[0]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *KinesisEgressRecord) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*KinesisEgressRecord) ProtoMessage() {}

func (x *KinesisEgressRecord) ProtoReflect() protoreflect.Message {
	mi := &file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_msgTypes[0]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use KinesisEgressRecord.ProtoReflect.Descriptor instead.
func (*KinesisEgressRecord) Descriptor() ([]byte, []int) {
	return file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_rawDescGZIP(), []int{0}
}

func (x *KinesisEgressRecord) GetPartitionKey() string {
	if x != nil {
		return x.PartitionKey
	}
	return ""
}

func (x *KinesisEgressRecord) GetValueBytes() []byte {
	if x != nil {
		return x.ValueBytes
	}
	return nil
}

func (x *KinesisEgressRecord) GetStream() string {
	if x != nil {
		return x.Stream
	}
	return ""
}

func (x *KinesisEgressRecord) GetExplicitHashKey() string {
	if x != nil {
		return x.ExplicitHashKey
	}
	return ""
}

var File_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto protoreflect.FileDescriptor

var file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_rawDesc = []byte{
	0x0a, 0x47, 0x73, 0x74, 0x61, 0x74, 0x65, 0x66, 0x75, 0x6e, 0x2d, 0x66, 0x6c, 0x69, 0x6e, 0x6b,
	0x2f, 0x73, 0x74, 0x61, 0x74, 0x65, 0x66, 0x75, 0x6e, 0x2d, 0x66, 0x6c, 0x69, 0x6e, 0x6b, 0x2d,
	0x69, 0x6f, 0x2f, 0x73, 0x72, 0x63, 0x2f, 0x6d, 0x61, 0x69, 0x6e, 0x2f, 0x70, 0x72, 0x6f, 0x74,
	0x6f, 0x62, 0x75, 0x66, 0x2f, 0x6b, 0x69, 0x6e, 0x65, 0x73, 0x69, 0x73, 0x2d, 0x65, 0x67, 0x72,
	0x65, 0x73, 0x73, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x12, 0x22, 0x6f, 0x72, 0x67, 0x2e, 0x61,
	0x70, 0x61, 0x63, 0x68, 0x65, 0x2e, 0x66, 0x6c, 0x69, 0x6e, 0x6b, 0x2e, 0x73, 0x74, 0x61, 0x74,
	0x65, 0x66, 0x75, 0x6e, 0x2e, 0x66, 0x6c, 0x69, 0x6e, 0x6b, 0x2e, 0x69, 0x6f, 0x22, 0x9f, 0x01,
	0x0a, 0x13, 0x4b, 0x69, 0x6e, 0x65, 0x73, 0x69, 0x73, 0x45, 0x67, 0x72, 0x65, 0x73, 0x73, 0x52,
	0x65, 0x63, 0x6f, 0x72, 0x64, 0x12, 0x23, 0x0a, 0x0d, 0x70, 0x61, 0x72, 0x74, 0x69, 0x74, 0x69,
	0x6f, 0x6e, 0x5f, 0x6b, 0x65, 0x79, 0x18, 0x01, 0x20, 0x01, 0x28, 0x09, 0x52, 0x0c, 0x70, 0x61,
	0x72, 0x74, 0x69, 0x74, 0x69, 0x6f, 0x6e, 0x4b, 0x65, 0x79, 0x12, 0x1f, 0x0a, 0x0b, 0x76, 0x61,
	0x6c, 0x75, 0x65, 0x5f, 0x62, 0x79, 0x74, 0x65, 0x73, 0x18, 0x02, 0x20, 0x01, 0x28, 0x0c, 0x52,
	0x0a, 0x76, 0x61, 0x6c, 0x75, 0x65, 0x42, 0x79, 0x74, 0x65, 0x73, 0x12, 0x16, 0x0a, 0x06, 0x73,
	0x74, 0x72, 0x65, 0x61, 0x6d, 0x18, 0x03, 0x20, 0x01, 0x28, 0x09, 0x52, 0x06, 0x73, 0x74, 0x72,
	0x65, 0x61, 0x6d, 0x12, 0x2a, 0x0a, 0x11, 0x65, 0x78, 0x70, 0x6c, 0x69, 0x63, 0x69, 0x74, 0x5f,
	0x68, 0x61, 0x73, 0x68, 0x5f, 0x6b, 0x65, 0x79, 0x18, 0x04, 0x20, 0x01, 0x28, 0x09, 0x52, 0x0f,
	0x65, 0x78, 0x70, 0x6c, 0x69, 0x63, 0x69, 0x74, 0x48, 0x61, 0x73, 0x68, 0x4b, 0x65, 0x79, 0x42,
	0x3c, 0x0a, 0x2c, 0x6f, 0x72, 0x67, 0x2e, 0x61, 0x70, 0x61, 0x63, 0x68, 0x65, 0x2e, 0x66, 0x6c,
	0x69, 0x6e, 0x6b, 0x2e, 0x73, 0x74, 0x61, 0x74, 0x65, 0x66, 0x75, 0x6e, 0x2e, 0x66, 0x6c, 0x69,
	0x6e, 0x6b, 0x2e, 0x69, 0x6f, 0x2e, 0x67, 0x65, 0x6e, 0x65, 0x72, 0x61, 0x74, 0x65, 0x64, 0x50,
	0x01, 0x5a, 0x0a, 0x2e, 0x3b, 0x6d, 0x65, 0x73, 0x73, 0x61, 0x67, 0x65, 0x73, 0x62, 0x06, 0x70,
	0x72, 0x6f, 0x74, 0x6f, 0x33,
}

var (
	file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_rawDescOnce sync.Once
	file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_rawDescData = file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_rawDesc
)

func file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_rawDescGZIP() []byte {
	file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_rawDescOnce.Do(func() {
		file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_rawDescData = protoimpl.X.CompressGZIP(file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_rawDescData)
	})
	return file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_rawDescData
}

var file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_msgTypes = make([]protoimpl.MessageInfo, 1)
var file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_goTypes = []interface{}{
	(*KinesisEgressRecord)(nil), // 0: org.apache.flink.statefun.flink.io.KinesisEgressRecord
}
var file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_depIdxs = []int32{
	0, // [0:0] is the sub-list for method output_type
	0, // [0:0] is the sub-list for method input_type
	0, // [0:0] is the sub-list for extension type_name
	0, // [0:0] is the sub-list for extension extendee
	0, // [0:0] is the sub-list for field type_name
}

func init() { file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_init() }
func file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_init() {
	if File_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto != nil {
		return
	}
	if !protoimpl.UnsafeEnabled {
		file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_msgTypes[0].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*KinesisEgressRecord); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
	}
	type x struct{}
	out := protoimpl.TypeBuilder{
		File: protoimpl.DescBuilder{
			GoPackagePath: reflect.TypeOf(x{}).PkgPath(),
			RawDescriptor: file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_rawDesc,
			NumEnums:      0,
			NumMessages:   1,
			NumExtensions: 0,
			NumServices:   0,
		},
		GoTypes:           file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_goTypes,
		DependencyIndexes: file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_depIdxs,
		MessageInfos:      file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_msgTypes,
	}.Build()
	File_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto = out.File
	file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_rawDesc = nil
	file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_goTypes = nil
	file_statefun_flink_statefun_flink_io_src_main_protobuf_kinesis_egress_proto_depIdxs = nil
}
