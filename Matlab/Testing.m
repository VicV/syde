% Master Drone Testing
clc;
close all;
clear;
%Read Background Image
background = imread('noObject1.jpg');
%Read Current Frame
currentFrame = imread('object1.jpg');
%Display Background and Foreground
subplot(2,2,1);imshow(background);title('Background Surface');
subplot(2,2,2);imshow(currentFrame);title('Current Frame');
%Convert RGB 2 HSV Color conversion
[background_hsv]=round(rgb2hsv(background));
[currentFrame_hsv]=round(rgb2hsv(currentFrame));
out = background_hsv-currentFrame_hsv;
%Convert RGB 2 GRAY
out=rgb2gray(out);
%Read Rows and Columns of the Image
[rows, cols]=size(out);
binaryImage = zeros(size(out));
%Convert to Binary Image
for i=1:rows
    for j=1:cols
        if out(i,j) >0
            binaryImage(i,j)=1;
        else
            binaryImage(i,j)=0;
        end
    end
end
%Apply median filter to remove noise
filteredImage=medfilt2(binaryImage,[5 5]);
%Boundary Label the Filtered Image
[L, num]=bwlabel(filteredImage);
STATS=regionprops(L,'all');
cc=[];
removed=0;
%Remove the noisy regions 
for i=1:num
    dd=STATS(i).Area;
    if (dd < 200)
        L(L==i)=0;
        removed = removed + 1;
        num=num-1;
    end
end
[L2, num2]=bwlabel(L);
% Trace region boundaries in a binary image.
[B,L,N,A] = bwboundaries(L2);
%Display results
subplot(2,2,3),  imshow(L2);title('Background Detected');
subplot(2,2,4),  imshow(L2);title('Blob Detected');
hold on;
for k=1:length(B),
    if(~sum(A(k,:)))
        boundary = B{k};
        plot(boundary(:,2), boundary(:,1), 'r','LineWidth',2);
        for l=find(A(:,k))
            boundary = B{l};
            plot(boundary(:,2), boundary(:,1), 'g','LineWidth',2);
        end
    end
end

[pw, ph] = calculatePixelDensity(imread('noObject1.jpg'), 59.02, 43.5, 0.16) 
% calculates it is 0.56 cm to the left and 0.3192 cm up
[row, col] = calculateGPSPoints(L, pw, ph)
plot(row, col, 'r.', 'MarkerSize', 18);
