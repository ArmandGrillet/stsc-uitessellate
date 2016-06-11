package uitessellate

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.Includes._
import scalafx.scene.Scene
import scalafxml.core.{NoDependencyResolver, FXMLView}

object View extends JFXApp {
    val view = FXMLView(getClass.getResource("/uitessellate.fxml"), NoDependencyResolver)
    stage = new PrimaryStage() {
        title = "Self-Tuning Spectral Clustering"
        resizable = false
        scene = new Scene(view)
    }
}
