package main.divers_services;

import java.io.File;

import static main.divers_services.conversionsSons.AiffToMp3Converter.convertirAiffToMp3;
import static main.divers_services.dropbox.DropBoxTransfert.transfertFichierFtp;

public class ConversionEtTransfertService
{
    public static void conversionEtTransfert(String fichierAiff,String dossierDropBox)
    {
        String dossierParent = new File(fichierAiff).getParentFile().getAbsolutePath();
        convertirAiffToMp3(fichierAiff,dossierParent);
        try
        {
            String fichierConverti = fichierAiff.replace(".aif",".mp3");
            transfertFichierFtp(fichierConverti, dossierDropBox);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

/*

    public static void main(String[] args)
    {
        conversionEtTransfert("C:\\Users\\Win\\Desktop\\c\\dj hustla - roots - Ending Party.aif","live");

    }

 */
}
