package org.rex

import org.scalatest.FunSuite

class CandGenTest extends FunSuite {

  import CandGenTest._

  test("Simple Sentence Candidate Generation") {

    val createdCandidates = sentenceCandGenAllWord.candidates(insurgentsDoc).toSet

    val diff = insurgentsCandidatesSentence.toSet.diff(createdCandidates)
    val intersection = insurgentsCandidatesSentence.toSet.intersect(createdCandidates)

    val test = diff.size == 0 && intersection.size == insurgentsCandidatesSentence.size && intersection.size == createdCandidates.size
    assert(test,
      s"""Candidates did not match. Expecting ${insurgentsCandidatesSentence.size} actually have ${createdCandidates.size} candidates.\n""" +
        s"""Difference: ${diff.mkString(" : ")}\nIntersection: ${intersection.mkString(" : ")}"""
    )
  }

  test("Coreference-based Candidate Generation") {

    val doc = {
      val es = NamedEntitySet.Default4Class.entSet
      NerDocChunker(es)(
        TextProcessorTest.makeProcessor(es).process("JohnJudy", johnJudyText)
      )
    }

    val mentions = doc.corefMentions.getOrElse(Seq.empty[Coref])

    val candidates = CorefCandGen(WordFilter.noKnownPunct, candidateFilter).candidates(doc)

    val actual = candidates.map(c => (c.queryW, c.answerW, c.inner)).toSet

    val errors = checkCandidates(actual, expectedJohnJudyCandidates)

    if (errors.nonEmpty)
      fail(s"""Incorrect actual candidates:\n${errors.mkString("\n")}""")
  }
}

object CandGenTest {

  val sentenceCandGenAllWord = SentenceCandGen(WordFilter.permitAll)

  val candidateFilter = new WordFilter {
    override def apply(s: Sentence)(i: Int): Boolean =
      WordFilter.noKnownPunct(s)(i) && (pronounOnlyFilter(s)(i) || nounOnlyfilter(s)(i))
  }

  val nounOnlyfilter = new WordFilter {
    override def apply(s: Sentence)(i: Int): Boolean =
      s.tags.exists(tags => tags(i) == "NN" || tags(i) == "NNS" || tags(i) == "NNP" || tags(i) == "NNPS")
  }

  val pronounOnlyFilter = new WordFilter {
    override def apply(s: Sentence)(i: Int): Boolean =
      s.tags.exists(tags => tags(i) == "PRP")
  }

  val sentenceCandGenNoKnownPunct = SentenceCandGen(WordFilter.noKnownPunct)

  import TextFeatuerizerTest._

  val insurgentsSentence = Sentence(insurgentsSeq)

  val insurgentsDoc = Document("insurgents", IndexedSeq(insurgentsSentence))

  val insurgentsCandidatesSentence = Seq(
    CandidateSentence(insurgentsSentence, 0, 1),
    CandidateSentence(insurgentsSentence, 0, 2),
    CandidateSentence(insurgentsSentence, 0, 3),
    CandidateSentence(insurgentsSentence, 0, 4),
    CandidateSentence(insurgentsSentence, 1, 0),
    CandidateSentence(insurgentsSentence, 1, 2),
    CandidateSentence(insurgentsSentence, 1, 3),
    CandidateSentence(insurgentsSentence, 1, 4),
    CandidateSentence(insurgentsSentence, 2, 0),
    CandidateSentence(insurgentsSentence, 2, 1),
    CandidateSentence(insurgentsSentence, 2, 3),
    CandidateSentence(insurgentsSentence, 2, 4),
    CandidateSentence(insurgentsSentence, 3, 0),
    CandidateSentence(insurgentsSentence, 3, 1),
    CandidateSentence(insurgentsSentence, 3, 2),
    CandidateSentence(insurgentsSentence, 3, 4),
    CandidateSentence(insurgentsSentence, 4, 0),
    CandidateSentence(insurgentsSentence, 4, 1),
    CandidateSentence(insurgentsSentence, 4, 2),
    CandidateSentence(insurgentsSentence, 4, 3)
  )

  val johnJudyText = "John drove to Judy’s house. He made her dinner."

  type SimpleCandSet = Set[(String, String, Seq[String])]

  val expectedJohnJudyCandidates = Set(
    ("John", "dinner", Seq("made", "her")),
    ("dinner", "John", Seq("made", "her")),
    ("He", "Judy", Seq("drove", "to")),
    ("Judy", "He", Seq("drove", "to")),
    ("He", "house", Seq("drove", "to", "Judy", "'s")),
    ("house", "He", Seq("drove", "to", "Judy", "'s"))
  )

  type Error = String

  def checkCandidates(actual: SimpleCandSet, expected: SimpleCandSet): List[Error] = {

    val diff = actual.diff(expected)
    val inter = actual.intersect(expected)

    val diffErr =
      if (diff.size > 0)
        List(s"""Difference: ${diff.mkString(" : ")}""")
      else
        List.empty[String]

    val interErr =
      if (inter.size != expected.size)
        List(s"""Intersection: ${inter.mkString(" : ")}""")
      else
        List.empty[String]

    diffErr ++ interErr
  }

}
