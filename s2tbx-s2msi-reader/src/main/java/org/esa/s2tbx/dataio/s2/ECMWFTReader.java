package org.esa.s2tbx.dataio.s2;

import org.apache.commons.io.FileUtils;
import org.esa.snap.core.datamodel.TiePointGrid;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class ECMWFTReader {

    List<TiePointGrid> tiePointGrids;

    public ECMWFTReader(Path path, Path cachedir) throws IOException {
        if (tiePointGrids != null)
            tiePointGrids.clear();
        tiePointGrids = new ArrayList<>();
        NetcdfFile ncfile = null;
        Path cacheFolderPath = cachedir;
        cacheFolderPath = cacheFolderPath.resolve("aux_ecmfwt");
        try {
            Files.createDirectory(cacheFolderPath);
        } catch (FileAlreadyExistsException exc) {
        }
        Path copyPath = cacheFolderPath.resolve(path.getFileName());
        try {
            Files.copy(path, copyPath, StandardCopyOption.REPLACE_EXISTING);
            ncfile = NetcdfFile.openInMemory(copyPath.toString());
            // String[] nameList = new String[] { "Total_column_water_vapour_surface", "Total_column_ozone_surface",
            //         "Mean_sea_level_pressure_surface", "Relative_humidity_isobaric",
            //         "10_metre_U_wind_component_surface", "10_metre_V_wind_component_surface" };
            // for (String name : nameList) {
            //     TiePointGrid tpGrid = getGrid(ncfile, name);
            //     if (tpGrid != null) {
            //         this.tiePointGrids.add(tpGrid);
            //     }
            // }
            List<GridPair> gridList = new ArrayList<GridPair>();
            gridList.add(new GridPair("Total_column_water_vapour_surface","tco3"));
            gridList.add(new GridPair("Total_column_ozone_surface","tcwv"));
            gridList.add(new GridPair("Mean_sea_level_pressure_surface","msl"));
            gridList.add(new GridPair("Relative_humidity_isobaric","r"));
            gridList.add(new GridPair("10_metre_U_wind_component_surface","10u"));
            gridList.add(new GridPair("10_metre_V_wind_component_surface","10v"));
            for(GridPair gridPair:gridList) {
                TiePointGrid tpGrid = getGrid(ncfile, gridPair);
                if (tpGrid != null) {
                    this.tiePointGrids.add(tpGrid);
                }
            }
        } catch (Exception ioe) {
            // Handle less-cool exceptions here
            ioe.printStackTrace();
        } finally {
            ncfile.close();
            FileUtils.deleteDirectory(cacheFolderPath.toFile());
        }
    }

    public List<TiePointGrid> getECMWFGrids() {
        return tiePointGrids;
    }

    public TiePointGrid getGrid(NetcdfFile ncfile, GridPair gridPair) throws IOException, InvalidRangeException {
        final Variable variable = ncfile.findVariable(null, gridPair.getKey());
        if (variable == null) {
            return null;
        }
        String description = variable.getDescription();
        if( description.contains("@"))
            description = description.split(" @")[0]+" at surface level provided by ECMWF";
        String units = variable.getUnitsString();
        final int[] shape = variable.getShape();
        float[] tiePoints = null;
        int width = shape[1];
        int height = shape[2];
        if(shape.length == 4)
        {
            width = shape[2];
            height = shape[3];
            int[] shape2 = new int[]{1,1,width,height};
            int[] origin = new int[]{0,0,0,0};
            tiePoints = (float[])variable.read(origin, shape2).getStorage();
        }else {
            tiePoints = (float[])variable.read().getStorage();
        }
        final TiePointGrid tiePointGrid = new TiePointGrid(gridPair.getName(), width, height, 0.5, 0.5, 1220, 1220,tiePoints);
        tiePointGrid.setUnit(units);
        tiePointGrid.setNoDataValue(Double.NaN);
        tiePointGrid.setNoDataValueUsed(true);
        tiePointGrid.setDescription(description);
        return tiePointGrid;
    }

    public class GridPair {
        private String key;
        private String name;
    
        public GridPair(String key, String name) {
            this.key = key;
            this.name = name;
        }

        protected final String getKey() {
            return key;
        }

        protected final String getName() {
            return name;
        }

    }
}
