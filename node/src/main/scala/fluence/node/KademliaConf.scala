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

package fluence.node

import com.typesafe.config.{ Config, ConfigFactory }

import scala.concurrent.duration.Duration

/**
 *
 * @param maxSiblingsSize Maximum number of siblings to store, e.g. K * Alpha
 * @param maxBucketSize Maximum size of a bucket, usually K
 * @param parallelism Parallelism factor (named Alpha in paper)
 * @param pingExpiresIn Duration to avoid too frequent ping requests, used in [[fluence.kad.Bucket.update()]]
 */
case class KademliaConf(
    maxBucketSize: Int,
    maxSiblingsSize: Int,
    parallelism: Int,
    pingExpiresIn: Duration
)

object KademliaConf {
  val ConfigPath = "fluence.network.kademlia"

  def read(path: String = ConfigPath, config: Config = ConfigFactory.load()): KademliaConf = {
    import net.ceedubs.ficus.Ficus._
    import net.ceedubs.ficus.readers.ArbitraryTypeReader._
    config.as[KademliaConf](path)
  }
}

