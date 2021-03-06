/*
 * Copyright (C) 2017  Fluence Labs Limited
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fluence.kad.grpc.client

import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{ Monad, ~> }
import com.google.protobuf.ByteString
import fluence.codec.Codec
import fluence.kad.protocol.{ Contact, KademliaRpc, Key, Node }
import fluence.kad.{ grpc, protocol }
import io.grpc.{ CallOptions, ManagedChannel }

import scala.concurrent.Future
import scala.language.{ higherKinds, implicitConversions }

/**
 * Implementation of KademliaClient over GRPC, with Task and Contact.
 *
 * @param stub GRPC Kademlia Stub
 */
class KademliaClient[F[_] : Monad](stub: grpc.KademliaGrpc.KademliaStub)(implicit
    codec: Codec[F, protocol.Node[Contact], grpc.Node],
    keyCodec: Codec[F, Key, Array[Byte]],
    run: Future ~> F) extends KademliaRpc[F, Contact] {

  import cats.instances.stream._

  private val streamCodec = Codec.codec[F, Stream[protocol.Node[Contact]], Stream[grpc.Node]]

  private val bsKey = keyCodec.direct.map(ByteString.copyFrom)

  /**
   * Ping the contact, get its actual Node status, or fail
   *
   * @return
   */
  override def ping(): F[Node[Contact]] =
    for {
      n ← run(stub.ping(grpc.PingRequest()))
      nc ← codec.decode(n)
    } yield nc

  /**
   * Perform a local lookup for a key, return K closest known nodes
   *
   * @param key Key to lookup
   * @return
   */
  override def lookup(key: Key, numberOfNodes: Int): F[Seq[Node[Contact]]] =
    for {
      k ← bsKey(key)
      res ← run(stub.lookup(grpc.LookupRequest(k, numberOfNodes)))
      resDec ← streamCodec.decode(res.nodes.toStream)
    } yield resDec

  /**
   * Perform an iterative lookup for a key, return K closest known nodes
   *
   * @param key Key to lookup
   * @return
   */
  override def lookupIterative(key: Key, numberOfNodes: Int): F[Seq[Node[Contact]]] =
    for {
      k ← bsKey(key)
      res ← run(stub.lookupIterative(grpc.LookupRequest(k, numberOfNodes)))
      resDec ← streamCodec.decode(res.nodes.toStream)
    } yield resDec

  /**
   * Perform a local lookup for a key, return K closest known nodes, going away from the second key
   *
   * @param key Key to lookup
   */
  override def lookupAway(key: Key, moveAwayFrom: Key, numberOfNodes: Int): F[Seq[Node[Contact]]] =
    for {
      k ← bsKey(key)
      moveAwayK ← bsKey(moveAwayFrom)
      res ← run(
        stub.lookupAway(grpc.LookupAwayRequest(k, moveAwayK, numberOfNodes))
      )
      resDec ← streamCodec.decode(res.nodes.toStream)
    } yield resDec
}

object KademliaClient {
  /**
   * Shorthand to register KademliaClient inside NetworkClient.
   *
   * @param channel     Channel to remote node
   * @param callOptions Call options
   */
  def register[F[_] : Monad]()(channel: ManagedChannel, callOptions: CallOptions)(implicit
    codec: Codec[F, protocol.Node[Contact], grpc.Node],
    keyCodec: Codec[F, Key, Array[Byte]],
    run: Future ~> F): KademliaClient[F] =
    new KademliaClient(new grpc.KademliaGrpc.KademliaStub(channel, callOptions))

}
