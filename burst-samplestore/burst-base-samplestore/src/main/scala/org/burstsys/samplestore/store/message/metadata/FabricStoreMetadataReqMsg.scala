/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.samplestore.store.message.metadata

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Input, Output}
import org.burstsys.fabric.net.message.{FabricNetMsg, FabricNetMsgContext}
import org.burstsys.fabric.topology.model.node.FabricNode
import org.burstsys.samplesource.service.MetadataParameters
import org.burstsys.samplestore.store.message.FabricStoreMetadataReqMsgType

trait FabricStoreMetadataReqMsg extends FabricNetMsg {
  def sourceName: String
  def metadata: MetadataParameters
}

/**
  * sent from server to client to initiate a metadata scan
  */
object FabricStoreMetadataReqMsg {

  def apply(senderKey: FabricNode, receiverKey: FabricNode, sourceName: String,
            metadata: MetadataParameters): FabricStoreMetadataReqMsg = {
    val m = FabricStoreMetadataReqMsgContext()
    m.senderKey = senderKey
    m.receiverKey = receiverKey
    m.sourceName = sourceName
    m.metadata = metadata
    m
  }

  def apply(buffer: Array[Byte]): FabricStoreMetadataReqMsg = {
    FabricStoreMetadataReqMsgContext().decode(buffer)
  }

}

final case
class FabricStoreMetadataReqMsgContext()
  extends FabricNetMsgContext(FabricStoreMetadataReqMsgType) with FabricStoreMetadataReqMsg {

  ////////////////////////////////////////////////////////////////////////////////////
  // STATE
  ////////////////////////////////////////////////////////////////////////////////////
  var sourceName: String = _

  var metadata: MetadataParameters = _

  ////////////////////////////////////////////////////////////////////////////////////
  // CODEC
  ////////////////////////////////////////////////////////////////////////////////////
  override
  def read(kryo: Kryo, input: Input): Unit = {
    super.read(kryo, input)
    sourceName = input.readString()
    metadata = readMap(kryo, input)
  }

  override
  def write(kryo: Kryo, output: Output): Unit = {
    super.write(kryo, output)
    output.writeString(sourceName)
    writeMap(kryo, output, metadata.asInstanceOf[Map[String, Serializable]])
  }

}
