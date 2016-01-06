function [pw, ph] = calculatePixelDensity(image, hFOV, vFOV, d)
% hFOV (horizontal field of view) and vFOV (vertical field of view) are given in degrees. Must convert to radians.
% d (distance from drone to ground) is given in m
    [h, w, z] = size(image);
    W = 2*d*tan(degtorad(hFOV/2)); % width of image area in m
    H = 2*d*tan(degtorad(vFOV/2)); % height of image area in m
    pw = w/(W*100); % distance value of the width of a pixel in cm
    ph = h/(H*100); % distance value of the height of a pixel in cm
end