package daa.kruskal;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;


public class Controller extends Application {

    public static void run(){ launch(); }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root= FXMLLoader.load(getClass().getResource("kruskal.fxml"));
        Scene scene= new Scene(root);
        stage.setResizable(false);
        stage.setTitle("Kruskal");
        stage.getIcons().add(new Image("file:../Images/kruskallogo.png"));
        stage.setScene(scene);
        stage.show();
    }
}
