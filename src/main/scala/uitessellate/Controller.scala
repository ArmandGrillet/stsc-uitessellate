package uitessellate

// UI
import java.awt.{Color, Graphics2D, Paint}
import java.awt.image.BufferedImage
import javafx.scene.{chart => jfxsc}
import org.jfree.chart.axis.{NumberTickUnit, TickUnits}
import scala.collection.mutable.ListBuffer
import scala.util.control._
import scalafx.embed.swing.SwingFXUtils
import scalafx.event.ActionEvent
import scalafx.scene.chart._
import scalafx.scene.control._
import scalafx.scene.control.Alert._
import scalafx.scene.image.ImageView
import scalafx.scene.layout.AnchorPane
import scalafx.scene.Node
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import scalafxml.core.{NoDependencyResolver, FXMLLoader}
import scalafxml.core.macros.sfxml

// Logic
import breeze.linalg._
import breeze.numerics._
import breeze.stats._
import breeze.plot._
import java.io.File
import stsc._

@sfxml
class Controller(private val root: AnchorPane, private val selectDataset: ChoiceBox[String], private val maxField: TextField, private val dataset: ImageView, private val clusters: ImageView) {
    private var displayedDataset = DenseMatrix.zeros[Double](0, 0)

    selectDataset.value.onChange {
        if (selectDataset.value.value.takeRight(4) == ".csv") {
            val dataset = new File(getClass.getResource("/" + selectDataset.value.value).getPath())
            displayedDataset = breeze.linalg.csvread(dataset)
            showDataset()
        }
    }

    def tessellate(event: ActionEvent) {
        var maxObservations = toInt(maxField.text.value).getOrElse(0)
        var ready = true

        if (ready && maxObservations < 2) {
            showAlert("Max has to be more than 2", "Having less than 2 observations per tile is not really interesting...")
            ready = false
        }

        if (displayedDataset.rows == 0) {
            showAlert("Needs a dataset", "Select a dataset before clustering it.")
            ready = false
        }

        if (ready && maxObservations > displayedDataset.rows) {
            showAlert("Max observations per tile is too big", "Max must be less than the number of observations in the cluster.")
            ready = false
        }

        if (ready) {
            val tessellations = stsc.tessellate(displayedDataset, maxObservations)

            val f = Figure()
            f.visible = false
            f.width = clusters.getBoundsInParent().getWidth().toInt
            f.height = clusters.getBoundsInParent().getHeight().toInt
            val p = f.subplot(0)
            p.xlim(min(displayedDataset(::, 0)) - 2, max(displayedDataset(::, 0)) + 2)
            p.ylim(min(displayedDataset(::, 1)) - 2, max(displayedDataset(::, 1)) + 2)
            p.title = "Tessellation tree"
            p += scatter(displayedDataset(::, 0), displayedDataset(::, 1), {(_:Int) => 0.01}, {(pos:Int) => Color.BLACK}) // Display the observations.

            for (i <- 0 until tessellations.rows) {
                var (modifiableMinX, modifiableMaxX, modifiableMinY, modifiableMaxY) = (tessellations(i, 0), tessellations(i, 1), tessellations(i, 2), tessellations(i, 3))
                if (modifiableMinX == scala.Double.NegativeInfinity) {
                    modifiableMinX = min(displayedDataset(::, 0)) - 2
                }
                if (modifiableMinY == scala.Double.NegativeInfinity) {
                    modifiableMinY = min(displayedDataset(::, 1)) - 2
                }
                if (modifiableMaxX == scala.Double.PositiveInfinity) {
                    modifiableMaxX = max(displayedDataset(::, 0)) + 2
                }
                if (modifiableMaxY == scala.Double.PositiveInfinity) {
                    modifiableMaxY = max(displayedDataset(::, 1)) + 2
                }

                val x = linspace(modifiableMinX, modifiableMaxX)
                val top = DenseVector.fill(x.length){modifiableMaxY}
                val bottom = DenseVector.fill(x.length){modifiableMinY}
                p += plot(x, top)
                p += plot(x, bottom)

                val y = linspace(modifiableMinY, modifiableMaxY)
                val left = DenseVector.fill(y.length){modifiableMinX}
                val right = DenseVector.fill(y.length){modifiableMaxX}
                p += plot(left, y)
                p += plot(right, y)
            }

            clusters.image = SwingFXUtils.toFXImage(imageToFigure(f), null)
        }
    }

    def loadDataset(event: ActionEvent) {
        val fileChooser = new FileChooser {
            title = "Choose dataset"
            extensionFilters.add(new ExtensionFilter("CSV Files", "*.csv"))
        }
        val selectedFile = fileChooser.showOpenDialog(root.getScene().getWindow())
        if (selectedFile != null) {
            val dataset = breeze.linalg.csvread(selectedFile)
            if (dataset.cols > 2) {
                showAlert("Too many dimensions", "There are " + dataset.cols + "in your CSV, we only need 2.")
            } else {
                selectDataset.value = "Select dataset"
                displayedDataset = dataset
                showDataset()
            }
        }
    }

    private def imageToFigure(f: Figure): BufferedImage = {
        val image = new BufferedImage(f.width, f.height, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        f.drawPlots(g2d)
        g2d.dispose
        return image
    }

    private def showAlert(header: String, content: String) {
        new Alert(AlertType.Error) {
            title = "Error"
            headerText = header
            contentText = content
        }.showAndWait()
    }

    private def showDataset() {
        val f = Figure()
        f.visible = false
        f.width = dataset.getBoundsInParent().getWidth().toInt
        f.height = dataset.getBoundsInParent().getHeight().toInt
        val p = f.subplot(0)
        p.title = "Dataset"
        p += scatter(displayedDataset(::, 0), displayedDataset(::, 1), {(_:Int) => 0.01}, {(pos:Int) => Color.BLACK}) // Display the observations.
        dataset.image = SwingFXUtils.toFXImage(imageToFigure(f), null)
    }

    private def toInt(s: String): Option[Int] = {
        try {
            Some(s.toInt)
        } catch {
            case e: Exception => None
        }
    }
}
