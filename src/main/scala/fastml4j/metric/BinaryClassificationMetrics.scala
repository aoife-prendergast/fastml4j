package fastml4j.metric

import fastml4j.classification.ClassificationModel
import fastml4j.util.Implicits._
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j



class BinaryClassificationMetrics(val outcome: INDArray, val predictedLabels: INDArray) {

  lazy val binTreshholds: Seq[Float] = predictedLabels
      .ravel
      .toArray
      .flatten
      .distinct
      .sorted
      .reverse
      .toSeq


  case class Confusion(predictedClass: Float, realClass: Float, qty: Int)

  lazy val confusionMatrixByThreshold: Seq[(Float, Seq[Confusion])] = {

    val zipped = Nd4j.hstack( predictedLabels, outcome)
      .toArray

    binTreshholds.map { threshold =>
      val confusion = zipped
        .map { case Array(predicted, real) =>
          val predictedClass = if( predicted >= threshold ) 1 else 0
          ((predictedClass, real), 1) }
      .groupBy(_._1)
      .toSeq
      .map{ case ((predictedClass, real), values) => Confusion(predictedClass, real, values.map( _._2).sum)}

      (threshold, confusion) }
  }


  def recallByTreshhold: Seq[(Float, Float)] = {

    val totalRealPositives: Int = confusionMatrixByThreshold.map (_._2).head
      .filter(_.realClass == 1)
      .map(_.qty)
      .sum

    confusionMatrixByThreshold.map { case (threshold, confusions) =>
        val truePositives = confusions
          .filter { c => c.predictedClass == c.realClass && c.realClass == 1 }
          .map(_.qty)
          .sum

      (threshold, if(truePositives == 0) 0 else truePositives.toFloat / totalRealPositives.toFloat)}
  }

  def precisionByTreshhold: Seq[(Float, Float)] = {

    confusionMatrixByThreshold.map { case (threshold, confusions) =>
      val truePositives = confusions
        .filter { c => c.predictedClass == c.realClass && c.realClass == 1 }
        .map(_.qty)
        .sum

      val allPositives = confusions
        .filter { c => c.predictedClass == 1 }
        .map(_.qty)
        .sum

      (threshold, if(truePositives == 0) 0 else truePositives.toFloat / allPositives.toFloat)}
  }

  def roc: Seq[(Float, Float)] = {

    val rocCurve = confusionMatrixByThreshold.map { case (threshold, confusions) =>
      val truePositives = confusions
        .filter { c => c.predictedClass == c.realClass && c.realClass == 1 }
        .map(_.qty)
        .sum

      val trueNegatives = confusions
        .filter { c => c.predictedClass == c.realClass && c.realClass == 0 }
        .map(_.qty)
        .sum

      val falsePositives = confusions
        .filter { c => c.predictedClass != c.realClass && c.predictedClass == 1 }
        .map(_.qty)
        .sum

      val falseNegatives = confusions
        .filter { c => c.predictedClass != c.realClass && c.predictedClass == 0 }
        .map(_.qty)
        .sum

      val tpr = if(truePositives == 0) 0.0f else truePositives.toFloat / (truePositives + falseNegatives)
      val fpr = if(falsePositives == 0) 0.0f else falsePositives.toFloat / (trueNegatives + falsePositives)


       (fpr, tpr)  }

    (0.0f, 0.0f) +: rocCurve :+ (1.0f, 1.0f)
  }

  def aucRoc: Float =
    roc.sortBy( x => (x._1, x._2))
      .sliding(2)
      .map { case Seq((leftX, leftY), (rightX, rightY)) =>  (rightX - leftX) * (rightY + leftY ) / 2 }
      .reduce(_+_)


}


