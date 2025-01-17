/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.vitals.git

import org.burstsys.vitals.reporter.VitalsReporter

import scala.language.postfixOps

/**
 * helper types/functions for GIT state
 */
private[vitals]
object VitalsGitReporter extends VitalsReporter {

  final val dName: String = "vitals-git"

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // LIFECYCLE
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  override def sample(sampleMs: Long): Unit = {
    newSample()
    super.sample(sampleMs)
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // REPORT
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  def report: String = {
    s"\tgit_branch=$branch, git_commit=$commitId \n"
  }

}
