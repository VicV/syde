function [distx, disty] = calculateGPSPoints(image, pw, ph)
% image is a binary image 
    [h, w, z] = size(image);
    counter = 0;
    top = 0;
    bottom = 0;
    left = 0;
    right = 0;
    % determine top of object
    for i=1:h
        counter = 0;
        for j=1:w
            if (image(i,j) == 1)
                counter = counter +1;
                if (counter > 10)
                   top = i;
                   break;
                end
            end
        end
        if (top ~= 0)
            break;
        end
    end
    % determine bottom of object
    for i=h:-1:1
        counter = 0;
        for j=1:w
            if (image(i,j) == 1)
                counter = counter +1;
                if (counter > 10)
                   bottom = i;
                   break;
                end
            end
        end
        if (bottom ~= 0)
            break;
        end
    end
    % determine left side of object
    for j=1:w
        counter = 0;
        for i=top:bottom
            if (image(i,j) == 1)
                counter = counter+1;
                if (counter > 10)
                   left = j;
                   break;
                end
            end
        end
        if (left ~= 0)
            break;
        end        
    end
    % determine right side of object
    for j=w:-1:1
        counter = 0;
        for i=top:bottom
            if (image(i,j) == 1)
                counter = counter +1;
                if (counter > 5)
                   right = j; 
                   break;
                end
            end
        end
        if (right ~= 0)
            break;
        end        
    end
    % determine location of centre of detected object
    row = round((bottom+top)/2); 
    col = round((right+left)/2);
    % calculate distance from the centre of the image to the centre of the
    % detected oject
    distx = (col - w/2)/pw; %distance in the x direction from the centre
    disty = (row - h/2)/ph; %distance in the y direction from the centre 
end