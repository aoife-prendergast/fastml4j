package fastml4j.loss

//
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.indexing.BooleanIndexing
import org.nd4j.linalg.indexing.conditions.Conditions
import fastml4j.util.Implicits._


/**
  * Created by rzykov on 31/05/17.
  */


class OLSLoss[T <: Regularisation](regularisation: T) extends Loss {

  override def loss(weights: INDArray, dataSet: DataSet): Float = {
    val predictedVsActual = (weights dot dataSet.getFeatures.T) - dataSet.getLabels.T

    (predictedVsActual.T * predictedVsActual).sumFloat / 2.0f / (dataSet.numExamples)  +
      regularisation.lossRegularisation(weights)
  }

  override def gradient(weights: INDArray, dataSet: DataSet): INDArray = {
    val main = dataSet.getFeatures.T dot ((dataSet.getFeatures dot weights.T) - dataSet.getLabels.T)

    (main / (dataSet.getFeatures.rows)).T + regularisation.gradientRegularisation(weights)
  }

}

